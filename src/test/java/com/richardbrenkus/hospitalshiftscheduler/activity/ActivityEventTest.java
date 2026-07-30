package com.richardbrenkus.hospitalshiftscheduler.activity;

import com.richardbrenkus.hospitalshiftscheduler.config.constants.ActivityType;
import com.richardbrenkus.hospitalshiftscheduler.config.constants.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityEventTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-08-05T08:15:00Z");
    private static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final RequestMetadata METADATA = new RequestMetadata("POST", "/admin/create", "127.0.0.1");

    @Test
    void success_shouldReturnEventWithSuccessTrueAndNullFailureReason() {
        ActivityEvent event = ActivityEvent.success(OCCURRED_AT, ActivityType.USER_CREATED, "alice", Role.ADMIN, "User", "42", "Created user", METADATA);

        assertThat(event.successful()).isTrue();
        assertThat(event.failureReason()).isNull();
        assertThat(event.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(event.actorUsername()).isEqualTo("alice");
        assertThat(event.actorRole()).isEqualTo(Role.ADMIN);
        assertThat(event.activityType()).isEqualTo(ActivityType.USER_CREATED);
        assertThat(event.targetType()).isEqualTo("User");
        assertThat(event.targetId()).isEqualTo("42");
        assertThat(event.description()).isEqualTo("Created user");
        assertThat(event.requestMetadata()).isSameAs(METADATA);
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    void failure_shouldReturnEventWithSuccessFalseAndPreserveReason() {
        ActivityEvent event = ActivityEvent.failure(OCCURRED_AT, ActivityType.USER_LOGIN_FAILED, "bob", Role.UNKNOWN, "Authentication", "bob", "Login failed", "Bad credentials", METADATA);

        assertThat(event.successful()).isFalse();
        assertThat(event.failureReason()).isEqualTo("Bad credentials");
        assertThat(event.actorRole()).isEqualTo(Role.UNKNOWN);
    }

    @Test
    void success_shouldGenerateUniqueEventIdPerInvocation() {
        ActivityEvent first = ActivityEvent.success(OCCURRED_AT, ActivityType.USER_CREATED, "a", Role.ADMIN, "User", "1", "d", METADATA);
        ActivityEvent second = ActivityEvent.success(OCCURRED_AT, ActivityType.USER_CREATED, "a", Role.ADMIN, "User", "1", "d", METADATA);

        assertThat(first.eventId()).isNotEqualTo(second.eventId());
    }

    @Test
    void canonicalConstructor_shouldPreserveExplicitEventId() {
        ActivityEvent event = new ActivityEvent(EVENT_ID, OCCURRED_AT, "alice", Role.ADMIN, ActivityType.USER_CREATED, "User", "1", "Created", true, null, METADATA);

        assertThat(event.eventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void canonicalConstructor_shouldRejectNullEventId() {
        assertThatThrownBy(() -> new ActivityEvent(null, OCCURRED_AT, "a", Role.ADMIN, ActivityType.USER_CREATED, "User", "1", "d", true, null, METADATA)).isInstanceOf(NullPointerException.class).hasMessageContaining("eventId");
    }

    @Test
    void canonicalConstructor_shouldRejectNullOccurredAt() {
        assertThatThrownBy(() -> new ActivityEvent(EVENT_ID, null, "a", Role.ADMIN, ActivityType.USER_CREATED, "User", "1", "d", true, null, METADATA)).isInstanceOf(NullPointerException.class).hasMessageContaining("occurredAt");
    }

    @Test
    void canonicalConstructor_shouldRejectNullActorUsername() {
        assertThatThrownBy(() -> new ActivityEvent(EVENT_ID, OCCURRED_AT, null, Role.ADMIN, ActivityType.USER_CREATED, "User", "1", "d", true, null, METADATA)).isInstanceOf(NullPointerException.class).hasMessageContaining("actorUsername");
    }

    @Test
    void canonicalConstructor_shouldRejectNullActorRole() {
        assertThatThrownBy(() -> new ActivityEvent(EVENT_ID, OCCURRED_AT, "a", null, ActivityType.USER_CREATED, "User", "1", "d", true, null, METADATA)).isInstanceOf(NullPointerException.class).hasMessageContaining("actorRole");
    }

    @Test
    void canonicalConstructor_shouldRejectNullActivityType() {
        assertThatThrownBy(() -> new ActivityEvent(EVENT_ID, OCCURRED_AT, "a", Role.ADMIN, null, "User", "1", "d", true, null, METADATA)).isInstanceOf(NullPointerException.class).hasMessageContaining("activityType");
    }

    @Test
    void canonicalConstructor_shouldRejectNullRequestMetadata() {
        assertThatThrownBy(() -> new ActivityEvent(EVENT_ID, OCCURRED_AT, "a", Role.ADMIN, ActivityType.USER_CREATED, "User", "1", "d", true, null, null)).isInstanceOf(NullPointerException.class).hasMessageContaining("requestMetadata");
    }

    @Test
    void canonicalConstructor_shouldAllowNullOptionalTargetAndDescription() {
        ActivityEvent event = new ActivityEvent(EVENT_ID, OCCURRED_AT, "a", Role.ADMIN, ActivityType.USER_CREATED, null, null, null, true, null, METADATA);

        assertThat(event.targetType()).isNull();
        assertThat(event.targetId()).isNull();
        assertThat(event.description()).isNull();
    }
}
