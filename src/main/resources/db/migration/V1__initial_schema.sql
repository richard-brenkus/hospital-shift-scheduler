CREATE TABLE activity_log
(
    id             BIGINT AUTO_INCREMENT NOT NULL,
    occurred_at    datetime              NOT NULL,
    actor_username VARCHAR(255)          NOT NULL,
    actor_role     VARCHAR(20)           NOT NULL,
    activity_type  VARCHAR(50)           NOT NULL,
    target_type    VARCHAR(255)          NULL,
    target_id      VARCHAR(255)          NULL,
    `description`  VARCHAR(1000)         NULL,
    successful     BIT(1)                NOT NULL,
    failure_reason VARCHAR(1000)         NULL,
    request_method VARCHAR(255)          NULL,
    request_path   VARCHAR(255)          NULL,
    client_ip      VARCHAR(255)          NULL,
    event_id       BINARY(16)            NOT NULL,
    CONSTRAINT pk_activity_log PRIMARY KEY (id)
);

CREATE TABLE cleanup_task
(
    id             BIGINT   NOT NULL,
    version        BIGINT   NOT NULL,
    is_active      BIT(1)   NOT NULL,
    execution_time datetime NULL,
    creation_time  datetime NULL,
    CONSTRAINT pk_cleanup_task PRIMARY KEY (id)
);

CREATE TABLE email_log
(
    month_year_string VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NULL,
    time_stamp        VARCHAR(255) NULL,
    CONSTRAINT pk_email_log PRIMARY KEY (month_year_string)
);

CREATE TABLE reminder_email_outbox
(
    id                       BIGINT AUTO_INCREMENT NOT NULL,
    event_id                 VARCHAR(36)           NOT NULL,
    idempotency_key          VARCHAR(150)          NOT NULL,
    source_task_id           BIGINT                NOT NULL,
    scheduled_execution_time datetime              NOT NULL,
    final_submission_day     date                  NOT NULL,
    recipient_user_id        BIGINT                NOT NULL,
    recipient_email          VARCHAR(320)          NOT NULL,
    recipient_display_name   VARCHAR(255)          NULL,
    status                   VARCHAR(20)           NOT NULL,
    attempt_count            INT                   NOT NULL,
    next_attempt_at          datetime              NOT NULL,
    claimed_at               datetime              NULL,
    claimed_by               VARCHAR(100)          NULL,
    claim_token              VARCHAR(36)           NULL,
    created_at               datetime              NOT NULL,
    sent_at                  datetime              NULL,
    dead_at                  datetime              NULL,
    last_failure_reason      VARCHAR(255)          NULL,
    version                  BIGINT                NOT NULL,
    CONSTRAINT pk_reminder_email_outbox PRIMARY KEY (id)
);

CREATE TABLE scheduled_events_profile
(
    id                                               BIGINT AUTO_INCREMENT NOT NULL,
    task_type                                        VARCHAR(255)          NULL,
    is_task_active                                   BIT(1)                NOT NULL,
    final_submission_year_month_day_hour_minute_code VARCHAR(255)          NULL,
    year_month_day_hour_minute_code                  VARCHAR(255)          NULL,
    date_time_input                                  VARCHAR(255)          NULL,
    event_counter                                    INT                   NULL,
    event_day                                        INT                   NULL,
    event_month                                      INT                   NULL,
    event_year                                       INT                   NULL,
    event_hour                                       INT                   NULL,
    event_minute                                     INT                   NULL,
    frequency_in_days                                INT                   NOT NULL,
    repetitions                                      INT                   NOT NULL,
    time_stamp                                       VARCHAR(255)          NULL,
    final_submission_day                             INT                   NOT NULL,
    time_string                                      VARCHAR(255)          NULL,
    CONSTRAINT pk_scheduled_events_profile PRIMARY KEY (id)
);

CREATE TABLE send_reminder_task
(
    id                            BIGINT   NOT NULL,
    version                       BIGINT   NOT NULL,
    is_active                     BIT(1)   NOT NULL,
    start_sending_time            datetime NULL,
    creation_time                 datetime NULL,
    frequency_in_days             INT      NOT NULL,
    repetitions                   INT      NOT NULL,
    final_request_submission_date date     NULL,
    counter                       INT      NOT NULL,
    CONSTRAINT pk_send_reminder_task PRIMARY KEY (id)
);

CREATE TABLE shift_preference
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    shift_type         INT                   NOT NULL,
    priority           INT                   NOT NULL,
    weekday_count      INT                   NOT NULL,
    weekend_count      INT                   NOT NULL,
    no_shift_requested BIT(1)                NOT NULL,
    any_date_selected  BIT(1)                NOT NULL,
    shift_request_id   BIGINT                NOT NULL,
    CONSTRAINT pk_shift_preference PRIMARY KEY (id)
);

CREATE TABLE shift_preference_dates_yes
(
    shift_preference_id BIGINT NOT NULL,
    dates_yes           date   NULL
);

CREATE TABLE shift_request
(
    shift_request_id BIGINT AUTO_INCREMENT NOT NULL,
    CONSTRAINT pk_shift_request PRIMARY KEY (shift_request_id)
);

CREATE TABLE shift_request_dates_no
(
    shift_request_id BIGINT NOT NULL,
    date_no          date   NULL
);

CREATE TABLE stored_schedule_shift_assignments
(
    date_id    BIGINT       NOT NULL,
    user_id    BIGINT       NULL,
    username   VARCHAR(255) NULL,
    name       VARCHAR(255) NULL,
    title      VARCHAR(255) NULL,
    shift_type INT          NOT NULL,
    CONSTRAINT pk_stored_schedule_shift_assignments PRIMARY KEY (date_id, shift_type)
);

CREATE TABLE stored_schedules
(
    date_id            BIGINT       NOT NULL,
    month_year_id      VARCHAR(255) NOT NULL,
    weekend_or_holiday BIT(1)       NULL,
    day_integer        INT          NULL,
    CONSTRAINT pk_stored_schedules PRIMARY KEY (date_id)
);

CREATE TABLE stored_user_stat_assigned_days
(
    user_stat_id BIGINT NOT NULL,
    day_of_month INT    NULL
);

CREATE TABLE stored_user_stat_requested_days
(
    user_stat_id BIGINT NOT NULL,
    day_of_month INT    NULL
);

CREATE TABLE stored_user_stats
(
    id                             BIGINT AUTO_INCREMENT NOT NULL,
    stat_month                     VARCHAR(7)            NOT NULL,
    user_id                        BIGINT                NULL,
    username                       VARCHAR(255)          NULL,
    name                           VARCHAR(255)          NULL,
    shift_type                     INT                   NOT NULL,
    requested_weekdays             INT                   NULL,
    requested_weekends             INT                   NULL,
    calculated_weekdays            INT                   NULL,
    calculated_weekends            INT                   NULL,
    remaining_weekdays             INT                   NULL,
    remaining_weekends             INT                   NULL,
    any_date_selected              BIT(1)                NULL,
    assigned_weekdays              INT                   NULL,
    assigned_weekends              INT                   NULL,
    assigned_total                 INT                   NULL,
    assigned_total_all_shift_types INT                   NULL,
    CONSTRAINT pk_stored_user_stats PRIMARY KEY (id)
);

CREATE TABLE user_allowed_shift_types
(
    user_id    BIGINT NOT NULL,
    shift_type INT    NULL
);

CREATE TABLE users
(
    user_id          BIGINT AUTO_INCREMENT NOT NULL,
    version          BIGINT                NOT NULL,
    name             VARCHAR(45)           NOT NULL,
    username         VARCHAR(45)           NOT NULL,
    email            VARCHAR(255)          NOT NULL,
    password         VARCHAR(256)          NOT NULL,
    note             VARCHAR(255)          NULL,
    creation_date    datetime              NULL,
    birthday         VARCHAR(255)          NULL,
    profession       VARCHAR(255)          NULL,
    title            VARCHAR(255)          NULL,
    enabled          BIT(1)                NOT NULL,
    `role`           VARCHAR(255)          NOT NULL,
    shift_request_id BIGINT                NULL,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT uc_users_name UNIQUE (name);

ALTER TABLE users
    ADD CONSTRAINT uc_users_shift_request UNIQUE (shift_request_id);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);

ALTER TABLE activity_log
    ADD CONSTRAINT uk_activity_log_event_id UNIQUE (event_id);

ALTER TABLE reminder_email_outbox
    ADD CONSTRAINT uk_reminder_email_outbox_event_id UNIQUE (event_id);

ALTER TABLE reminder_email_outbox
    ADD CONSTRAINT uk_reminder_email_outbox_idempotency_key UNIQUE (idempotency_key);

ALTER TABLE reminder_email_outbox
    ADD CONSTRAINT uk_reminder_email_outbox_occurrence_recipient UNIQUE (source_task_id, scheduled_execution_time, recipient_user_id);

CREATE INDEX idx_activity_log_activity_type ON activity_log (activity_type);

CREATE INDEX idx_activity_log_actor_username ON activity_log (actor_username);

CREATE INDEX idx_activity_log_occurred_at ON activity_log (occurred_at);

CREATE INDEX idx_reminder_email_outbox_claim ON reminder_email_outbox (status, claimed_at);

CREATE INDEX idx_reminder_email_outbox_dispatch ON reminder_email_outbox (status, next_attempt_at);

CREATE INDEX idx_stored_user_stats_shift_type ON stored_user_stats (shift_type);

CREATE INDEX idx_stored_user_stats_stat_month ON stored_user_stats (stat_month);

CREATE INDEX idx_stored_user_stats_user_id ON stored_user_stats (user_id);

ALTER TABLE shift_preference
    ADD CONSTRAINT FK_SHIFT_PREFERENCE_ON_SHIFT_REQUEST FOREIGN KEY (shift_request_id) REFERENCES shift_request (shift_request_id);

ALTER TABLE users
    ADD CONSTRAINT FK_USERS_ON_SHIFT_REQUEST FOREIGN KEY (shift_request_id) REFERENCES shift_request (shift_request_id);

ALTER TABLE shift_request_dates_no
    ADD CONSTRAINT fk_shift_request_dates_no_on_shift_request FOREIGN KEY (shift_request_id) REFERENCES shift_request (shift_request_id);

ALTER TABLE shift_preference_dates_yes
    ADD CONSTRAINT fk_shiftpreference_datesyes_on_shift_preference FOREIGN KEY (shift_preference_id) REFERENCES shift_preference (id);

ALTER TABLE stored_schedule_shift_assignments
    ADD CONSTRAINT fk_stored_schedule_shift_assignments_on_stored_schedule_day FOREIGN KEY (date_id) REFERENCES stored_schedules (date_id);

ALTER TABLE stored_user_stat_assigned_days
    ADD CONSTRAINT fk_stored_user_stat_assigned_days_on_user_stat_entity FOREIGN KEY (user_stat_id) REFERENCES stored_user_stats (id);

ALTER TABLE stored_user_stat_requested_days
    ADD CONSTRAINT fk_stored_user_stat_requested_days_on_user_stat_entity FOREIGN KEY (user_stat_id) REFERENCES stored_user_stats (id);

ALTER TABLE user_allowed_shift_types
    ADD CONSTRAINT fk_user_allowed_shift_types_on_user FOREIGN KEY (user_id) REFERENCES users (user_id);

ALTER TABLE reminder_email_outbox
    ADD CONSTRAINT chk_reminder_email_outbox_attempt_count CHECK (attempt_count >= 0);

INSERT INTO cleanup_task (id, version, is_active, execution_time, creation_time) VALUES (1,0,0,NULL,NULL);
INSERT INTO send_reminder_task (id, version, is_active, start_sending_time, creation_time, frequency_in_days, repetitions, final_request_submission_date, counter) VALUES (1,0,0,NULL,NULL,0,0,NULL,0);