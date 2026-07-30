package com.richardbrenkus.hospitalshiftscheduler.activity;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.Role;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityPublisherTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-08-05T08:00:00Z");

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RequestMetadataProvider requestMetadataProvider;

    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    private final Clock fixedClock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private ActivityPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new ActivityPublisher(eventPublisher, requestMetadataProvider, trustResolver, fixedClock);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishSuccess_shouldPublishImmediately_whenNoActiveTransaction() {
        givenSystemActor();

        publisher.publishSuccess(ActivityType.USER_CREATED, "User", "1", "Created");

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ActivityEvent event = captor.getValue();
        assertThat(event.successful()).isTrue();
        assertThat(event.actorUsername()).isEqualTo("SYSTEM");
        assertThat(event.actorRole()).isEqualTo(Role.SYSTEM);
        assertThat(event.activityType()).isEqualTo(ActivityType.USER_CREATED);
        assertThat(event.occurredAt()).isEqualTo(FIXED_INSTANT);
    }

    @Test
    void publishSuccess_shouldDeferPublish_whenTransactionIsActive() {
        givenSystemActor();
        TransactionSynchronizationManager.initSynchronization();
        try {
            simulateActiveTransaction();

            publisher.publishSuccess(ActivityType.USER_CREATED, "User", "1", "Created");

            verify(eventPublisher, never()).publishEvent(org.mockito.ArgumentMatchers.any(ActivityEvent.class));

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);

            // Trigger the after-commit callback and verify publish is now invoked
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(ActivityEvent.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishFailure_shouldIncludeFailureReasonAndProvidedMetadata() {
        SecurityContextHolder.clearContext();
        RequestMetadata providedMetadata = new RequestMetadata("POST", "/x", "1.2.3.4");

        publisher.publishFailure(ActivityType.USER_LOGIN_FAILED, "Authentication", "bob", "Login failed", "Bad credentials", providedMetadata);

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ActivityEvent event = captor.getValue();
        assertThat(event.successful()).isFalse();
        assertThat(event.failureReason()).isEqualTo("Bad credentials");
        assertThat(event.requestMetadata()).isEqualTo(providedMetadata);
    }

    @Test
    void publishFailure_shouldFallBackToProvidedMetadata_whenNullExplicitMetadata() {
        givenSystemActor();
        RequestMetadata contextMetadata = new RequestMetadata("GET", "/x", "9.9.9.9");
        org.mockito.Mockito.when(requestMetadataProvider.current()).thenReturn(contextMetadata);

        publisher.publishFailure(ActivityType.USER_LOGIN_FAILED, "Authentication", "bob", "Login failed", "Bad credentials", null);

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().requestMetadata()).isEqualTo(contextMetadata);
    }

    @Test
    void publishSuccess_shouldMapAuthenticatedUserToRoleAndUsername() {
        UserDetails principal = User.withUsername("alice").password("x").authorities(new SimpleGrantedAuthority("ROLE_ADMIN")).build();
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "x", principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        org.mockito.Mockito.when(requestMetadataProvider.current()).thenReturn(RequestMetadata.system());

        publisher.publishSuccess(ActivityType.USER_CREATED, "User", "1", "Created");

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        ActivityEvent event = captor.getValue();
        assertThat(event.actorUsername()).isEqualTo("alice");
        assertThat(event.actorRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void publishSuccess_shouldFallBackToSystem_whenAnonymousAuthentication() {
        Authentication anonymous = new AnonymousAuthenticationToken("anon-key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(anonymous);
        org.mockito.Mockito.when(requestMetadataProvider.current()).thenReturn(RequestMetadata.system());

        publisher.publishSuccess(ActivityType.USER_CREATED, "User", "1", "Created");

        ArgumentCaptor<ActivityEvent> captor = ArgumentCaptor.forClass(ActivityEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().actorUsername()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().actorRole()).isEqualTo(Role.SYSTEM);
    }

    @Test
    void publishSuccess_shouldSwallowExceptions_whenPublishThrows() {
        givenSystemActor();
        doThrow(new RuntimeException("boom")).when(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(ActivityEvent.class));

        // Must not propagate to the caller (business flow already committed)
        publisher.publishSuccess(ActivityType.USER_CREATED, "User", "1", "Created");

        verify(eventPublisher).publishEvent(org.mockito.ArgumentMatchers.any(ActivityEvent.class));
    }

    private void givenSystemActor() {
        SecurityContextHolder.clearContext();
        org.mockito.Mockito.when(requestMetadataProvider.current()).thenReturn(RequestMetadata.system());
    }

    /**
     * Registers a no-op transaction synchronization to make
     * {@link TransactionSynchronizationManager#isActualTransactionActive()} return
     * true without requiring a full DataSource.
     */
    private static void simulateActiveTransaction() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
    }
}
