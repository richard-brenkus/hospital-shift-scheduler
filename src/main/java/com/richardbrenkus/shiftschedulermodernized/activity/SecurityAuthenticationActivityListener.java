package com.richardbrenkus.shiftschedulermodernized.activity;

import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityAuthenticationActivityListener {

    private final ActivityPublisher activityPublisher;
    private final RequestMetadataProvider requestMetadataProvider;

    @EventListener
    public void onLoginSucceeded(AuthenticationSuccessEvent event) {
        String username = safeUsername(event.getAuthentication().getName());

        activityPublisher.publishSuccess(
                ActivityType.USER_LOGIN_SUCCEEDED,
                "Authentication",
                username,
                "User login succeeded"
        );
    }

    @EventListener
    public void onLoginFailed(AbstractAuthenticationFailureEvent event) {
        String username = safeUsername(event.getAuthentication().getName());

        activityPublisher.publishFailure(
                ActivityType.USER_LOGIN_FAILED,
                "Authentication",
                username,
                "User login failed",
                this.safeFailureReason(event),
                requestMetadataProvider.current()
        );
    }

    @EventListener
    public void onLogoutSucceeded(LogoutSuccessEvent event) {
        
        String username = "UNKNOWN";
        if(event.getAuthentication() != null)
            username = safeUsername(event.getAuthentication().getName());

        activityPublisher.publishSuccess(
                ActivityType.USER_LOGOUT,
                "Authentication",
                username,
                "User logout succeeded"
        );
    }

    private String safeUsername(String username) {
        if (username == null || username.isBlank()) {
            return "UNKNOWN";
        }

        String trimmed = username.trim();
        return trimmed.length() <= 100
                ? trimmed
                : trimmed.substring(0, 100);
    }

    @SuppressWarnings("unused")
    private String safeFailureReason(AbstractAuthenticationFailureEvent event) {
        return event.getClass().getSimpleName();
    }
}

