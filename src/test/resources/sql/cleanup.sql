SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM activity_log;
DELETE FROM cleanup_task;
DELETE FROM email_log;
DELETE FROM reminder_email_outbox;
DELETE FROM scheduled_events_profile;
DELETE FROM send_reminder_task;

DELETE FROM shift_preference_dates_yes;
DELETE FROM shift_preference;
DELETE FROM shift_request_dates_no;
DELETE FROM user_allowed_shift_types;
DELETE FROM shift_request;

DELETE FROM stored_schedule_shift_assignments;
DELETE FROM stored_user_stat_assigned_days;
DELETE FROM stored_user_stat_requested_days;
DELETE FROM stored_user_stats;
DELETE FROM stored_schedules;

DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

