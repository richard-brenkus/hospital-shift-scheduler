package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityPublisher {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final ApplicationEventPublisher eventPublisher;
    private final RequestMetadataProvider requestMetadataProvider;

    public void publishSuccess(
            ActivityType activityType,
            String targetType,
            String targetId,
            String description
    ) {
        Objects.requireNonNull(
                activityType,
                "activityType must not be null"
        );

        Actor actor = currentActor();
        RequestMetadata metadata =
                requestMetadataProvider.current();

        ActivityEvent event = ActivityEvent.success(
                activityType,
                actor.username(),
                actor.role(),
                targetType,
                targetId,
                description,
                metadata
        );

        publishAfterCommitOrImmediately(event);
    }

    public void publishFailure(
            ActivityType activityType,
            String targetType,
            String targetId,
            String description,
            String failureReason,
            RequestMetadata requestMetadata
    ) {
        Objects.requireNonNull(
                activityType,
                "activityType must not be null"
        );

        Actor actor = currentActor();

        RequestMetadata metadata =
                requestMetadata == null
                        ? requestMetadataProvider.current()
                        : requestMetadata;

        ActivityEvent event = ActivityEvent.failure(
                activityType,
                actor.username(),
                actor.role(),
                targetType,
                targetId,
                description,
                failureReason,
                metadata
        );

        publishAfterCommitOrImmediately(event);
    }

    private void publishAfterCommitOrImmediately(
            ActivityEvent event
    ) {
        if (TransactionSynchronizationManager
                .isActualTransactionActive()
                && TransactionSynchronizationManager
                .isSynchronizationActive()) {

            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    publishSafely(event);
                                }
                            }
                    );
            return;
        }

        publishSafely(event);
    }

    private void publishSafely(
            ActivityEvent event
    ) {
        try {
            eventPublisher.publishEvent(event);
        } catch (RuntimeException exception) {
            log.error(
                    "Activity event {} could not be published",
                    event,
                    exception
            );
        }
    }

    private Actor currentActor() {
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return new Actor(
                    SYSTEM_ACTOR,
                    Role.SYSTEM
            );
        }

        Role role = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::removeRolePrefix)
                .map(this::toRoleOrNull)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(Role.SYSTEM);

        return new Actor(
                authentication.getName(),
                role
        );
    }

    private String removeRolePrefix(
            String authority
    ) {
        return authority != null
                && authority.startsWith("ROLE_")
                ? authority.substring(5)
                : authority;
    }

    private Role toRoleOrNull(
            String value
    ) {
        try {
            return Role.valueOf(value);
        } catch (IllegalArgumentException
                 | NullPointerException exception) {
            return null;
        }
    }

    private record Actor(
            String username,
            Role role
    ) {
    }
}