package com.richardbrenkus.shiftschedulermodernized.activity.kafka;

import com.richardbrenkus.shiftschedulermodernized.activity.ActivityEvent;
import com.richardbrenkus.shiftschedulermodernized.activity.RequestMetadata;
import com.richardbrenkus.shiftschedulermodernized.config.constants.ActivityType;
import com.richardbrenkus.shiftschedulermodernized.config.constants.Role;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActivityKafkaMapperTest {

    private static final UUID EVENT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final Instant OCCURRED_AT = Instant.parse("2026-08-05T08:00:00Z");

    private final ActivityKafkaMapper mapper = new ActivityKafkaMapper();

    @Test
    void toMessage_shouldCopyAllFieldsAndConvertEnumsToStableStrings() {
        ActivityEvent event = new ActivityEvent(
                EVENT_ID,
                OCCURRED_AT,
                "alice",
                Role.ADMIN,
                ActivityType.USER_CREATED,
                "User",
                "42",
                "Created user",
                true,
                null,
                new RequestMetadata("POST", "/admin/add", "1.2.3.4")
        );

        ActivityKafkaMessage message = mapper.toMessage(event);

        assertThat(message.eventId()).isEqualTo(EVENT_ID);
        assertThat(message.occurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(message.activityType()).isEqualTo("USER_CREATED");
        assertThat(message.actorUsername()).isEqualTo("alice");
        assertThat(message.actorRole()).isEqualTo("ADMIN");
        assertThat(message.targetType()).isEqualTo("User");
        assertThat(message.targetId()).isEqualTo("42");
        assertThat(message.description()).isEqualTo("Created user");
        assertThat(message.successful()).isTrue();
        assertThat(message.failureReason()).isEmpty();
        assertThat(message.requestMethod()).isEqualTo("POST");
        assertThat(message.requestPath()).isEqualTo("/admin/add");
        assertThat(message.clientIp()).isEqualTo("1.2.3.4");
    }

    @Test
    void toMessage_shouldReplaceNullPayloadStringsWithEmptyString() {
        // targetType / targetId / description / failureReason are optional payload
        // fields on ActivityEvent. The mapper is expected to normalise them
        // to empty strings so consumers can treat them as always-present.
        ActivityEvent event = new ActivityEvent(
                EVENT_ID,
                OCCURRED_AT,
                "alice",
                Role.ADMIN,
                ActivityType.USER_CREATED,
                null,
                null,
                null,
                true,
                null,
                RequestMetadata.system()
        );

        ActivityKafkaMessage message = mapper.toMessage(event);

        assertThat(message.targetType()).isEmpty();
        assertThat(message.targetId()).isEmpty();
        assertThat(message.description()).isEmpty();
        assertThat(message.failureReason()).isEmpty();
    }

    @Test
    void toMessage_shouldPassThroughRequestMetadataFieldsWithoutRewriting() {
        // requestMethod / requestPath / clientIp originate from
        // RequestMetadataProvider, which already substitutes "SYSTEM" or
        // "UNKNOWN" for missing values. The mapper trusts that and forwards
        // the sub-fields verbatim rather than re-mapping them.
        ActivityEvent event = new ActivityEvent(
                EVENT_ID,
                OCCURRED_AT,
                "alice",
                Role.ADMIN,
                ActivityType.USER_CREATED,
                "User",
                "42",
                "Created user",
                true,
                null,
                new RequestMetadata("GET", "/path", "10.0.0.1")
        );

        ActivityKafkaMessage message = mapper.toMessage(event);

        assertThat(message.requestMethod()).isEqualTo("GET");
        assertThat(message.requestPath()).isEqualTo("/path");
        assertThat(message.clientIp()).isEqualTo("10.0.0.1");
    }

    @Test
    void toMessage_shouldPreserveEventIdVerbatim() {
        ActivityEvent event = ActivityEvent.success(
                OCCURRED_AT, ActivityType.USER_CREATED, "alice", Role.ADMIN,
                "User", "1", "d",
                new RequestMetadata("GET", "/", "1.1.1.1")
        );

        ActivityKafkaMessage message = mapper.toMessage(event);

        assertThat(message.eventId()).isEqualTo(event.eventId());
    }

    @Test
    void toMessage_shouldNotEmbedJpaEntitiesOrRuntimeEnumTypes() {
        ActivityEvent event = new ActivityEvent(
                EVENT_ID,
                OCCURRED_AT,
                "alice",
                Role.ADMIN,
                ActivityType.USER_CREATED,
                "User",
                "42",
                "Created user",
                true,
                null,
                new RequestMetadata("POST", "/admin/add", "1.2.3.4")
        );

        ActivityKafkaMessage message = mapper.toMessage(event);

        // Every field must be a primitive, String, UUID or Instant — no application enum,
        // no entity type and no request-metadata sub-record leak.
        assertThat(message.activityType()).isInstanceOf(String.class);
        assertThat(message.actorRole()).isInstanceOf(String.class);
        assertThat(message).extracting(
                ActivityKafkaMessage::eventId,
                ActivityKafkaMessage::occurredAt,
                ActivityKafkaMessage::activityType,
                ActivityKafkaMessage::actorRole
        ).allSatisfy(value -> assertThat(value.getClass().getPackageName())
                .doesNotStartWith("com.richardbrenkus.shiftschedulermodernized"));
    }

    @Test
    void toMessage_shouldRejectNullEvent() {
        assertThatThrownBy(() -> mapper.toMessage(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ActivityEvent");
    }
}
