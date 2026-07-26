SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM shift_preference_dates_yes;
DELETE FROM shift_request_dates_no;
DELETE FROM user_allowed_shift_types;
DELETE FROM stored_schedule_shift_assignments;
DELETE FROM stored_user_stat_assigned_days;
DELETE FROM stored_user_stat_requested_days;
DELETE FROM shift_preference;
DELETE FROM user_stat;
DELETE FROM stored_user_stats;
DELETE FROM schedule_entry;
DELETE FROM send_reminder_task;
DELETE FROM cleanup_task;
DELETE FROM scheduled_events_profile;
DELETE FROM monthly_schedule;
DELETE FROM stored_schedules;
DELETE FROM shift_request;
DELETE FROM email_log;
DELETE FROM users;

SET FOREIGN_KEY_CHECKS = 1;

DELETE from users where username != 'alajos';