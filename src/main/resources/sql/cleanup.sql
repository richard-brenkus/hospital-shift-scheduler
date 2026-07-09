SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE shift_preference_dates_yes;
TRUNCATE TABLE shift_request_dates_no;
TRUNCATE TABLE user_allowed_shift_types;
TRUNCATE TABLE stored_schedule_shift_assignments;
TRUNCATE TABLE stored_calendar_shift_assignments;
TRUNCATE TABLE stored_user_stat_assigned_days;
TRUNCATE TABLE stored_user_stat_requested_days;
TRUNCATE TABLE user_stat_entity_assigned_date_days;
TRUNCATE TABLE user_stat_entity_requested_date_days;
TRUNCATE TABLE shift_preference;
TRUNCATE TABLE user_stat;
TRUNCATE TABLE stored_user_stats;
TRUNCATE TABLE schedule_entry;
TRUNCATE TABLE send_reminder_task;
TRUNCATE TABLE cleanup_task;
TRUNCATE TABLE scheduled_events_profile;
TRUNCATE TABLE monthly_schedule;
TRUNCATE TABLE stored_schedules;
TRUNCATE TABLE stored_calendars;
TRUNCATE TABLE shift_request;
TRUNCATE TABLE email_log;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;