-- Stored schedule days for the last days of July 2026 (day 30 and 31).
-- Used to verify ScheduleRuleService.loadPreviousStoredScheduleDays picks the correct
-- dateIds when computing the previous-month gap for the first day of August 2026.

DELETE FROM stored_schedule_shift_assignments;
DELETE FROM stored_schedules;

INSERT INTO stored_schedules (date_id, month_year_id, weekend_or_holiday, day_integer)
VALUES (20260730, '07/2026', FALSE, 30);
INSERT INTO stored_schedule_shift_assignments (date_id, shift_type, user_id, username, name, title)
VALUES (20260730, 1, 4001, 'previous.month.user', 'Previous Month User', 'MUDr.');

INSERT INTO stored_schedules (date_id, month_year_id, weekend_or_holiday, day_integer)
VALUES (20260731, '07/2026', FALSE, 31);
INSERT INTO stored_schedule_shift_assignments (date_id, shift_type, user_id, username, name, title)
VALUES (20260731, 2, 4002, 'other.previous', 'Other Previous', '');
