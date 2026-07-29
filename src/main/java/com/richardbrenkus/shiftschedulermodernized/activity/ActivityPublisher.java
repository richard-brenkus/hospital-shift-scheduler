package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityPublisher {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final ApplicationEventPublisher eventPublisher;
    private final RequestMetadataProvider requestMetadataProvider;
    private final AuthenticationTrustResolver authenticationTrustResolver;
    private final Clock applicationClock;

    public void publishSuccess(ActivityType activityType, String targetType, String targetId, String description) {
        try {
            ActivityEvent event = createSuccessEvent(activityType, targetType, targetId, description);

            publishAfterCommitOrImmediately(event);

        } catch (RuntimeException exception) {
            log.error("Could not prepare or publish success activity of type {}", activityType, exception);
        }
    }

    public void publishFailure(ActivityType activityType, String targetType, String targetId, String description, String failureReason, RequestMetadata requestMetadata) {
        try {
            ActivityEvent event = createFailureEvent(activityType, targetType, targetId, description, failureReason, requestMetadata);

            publishAfterCommitOrImmediately(event);

        } catch (RuntimeException exception) {
            log.error("Could not prepare or publish failure activity of type {}", activityType, exception);
        }
    }

    private void publishAfterCommitOrImmediately(ActivityEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive() && TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publishSafely(event);
                }
            });
            return;
        }

        publishSafely(event);
    }

    private void publishSafely(ActivityEvent event) {
        try {
            eventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.error("Activity event {} could not be published", event, exception);
        }
    }

    private Actor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!authenticationTrustResolver.isAuthenticated(authentication)) {
            return new Actor(SYSTEM_ACTOR, Role.SYSTEM);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof UserDetails) && !(principal instanceof String)) {

            return new Actor(SYSTEM_ACTOR, Role.UNKNOWN);
        }

        Role role = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).map(this::removeRolePrefix).map(this::toRoleOrNull).filter(Objects::nonNull).findFirst().orElse(Role.UNKNOWN);

        return new Actor(authentication.getName(), role);
    }

    private String removeRolePrefix(String authority) {
        return authority != null && authority.startsWith("ROLE_") ? authority.substring(5) : authority;
    }

    private Role toRoleOrNull(String value) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return null;
        }
    }

    private record Actor(String username, Role role) {
    }

    private ActivityEvent createSuccessEvent(ActivityType activityType, String targetType, String targetId, String description) {
        Objects.requireNonNull(activityType, "activityType must not be null");

        Actor actor = currentActor();
        RequestMetadata metadata = requestMetadataProvider.current();
        Instant occurredAt = Instant.now(applicationClock);

        return ActivityEvent.success(occurredAt, activityType, actor.username(), actor.role(), targetType, targetId, description, metadata);
    }

    private ActivityEvent createFailureEvent(ActivityType activityType, String targetType, String targetId, String description, String failureReason, RequestMetadata requestMetadata) {
        Objects.requireNonNull(activityType, "activityType must not be null");

        Actor actor = currentActor();

        RequestMetadata metadata = requestMetadata == null ? requestMetadataProvider.current() : requestMetadata;

        Instant occurredAt = Instant.now(applicationClock);

        return ActivityEvent.failure(occurredAt, activityType, actor.username(), actor.role(), targetType, targetId, description, failureReason, metadata);
    }
}