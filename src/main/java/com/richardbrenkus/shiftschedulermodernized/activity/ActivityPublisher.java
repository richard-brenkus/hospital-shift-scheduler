package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
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
        Actor actor = currentActor();

        eventPublisher.publishEvent(
                ActivityEvent.success(
                        activityType,
                        actor.username(),
                        actor.role(),
                        targetType,
                        targetId,
                        description,
                        requestMetadataProvider.current()
                )
        );
    }

    private Actor currentActor() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return new Actor(SYSTEM_ACTOR, Role.SYSTEM);
        }

        Role role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(this::removeRolePrefix)
                .map(this::toRoleOrNull)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(Role.SYSTEM);

        return new Actor(authentication.getName(), role);
    }

    private String removeRolePrefix(String authority) {
        return authority != null && authority.startsWith("ROLE_")
                ? authority.substring(5)
                : authority;
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
}
