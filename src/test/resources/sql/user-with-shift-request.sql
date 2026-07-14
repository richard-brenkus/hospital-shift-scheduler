SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM shift_preference_dates_yes;
DELETE FROM shift_request_dates_no;
DELETE FROM user_allowed_shift_types;
DELETE FROM stored_schedule_shift_assignments;
DELETE FROM stored_user_stat_assigned_days;
DELETE FROM stored_user_stat_requested_days;
DELETE FROM shift_preference;
DELETE FROM stored_user_stats;
DELETE FROM stored_schedules;
DELETE FROM users;
DELETE FROM shift_request;

INSERT INTO shift_request (shift_request_id) VALUES (3001);

INSERT INTO users (user_id, name, username, email, password, note, creation_date, birthday, profession, title, enabled, role, version, shift_request_id)
VALUES (3001, 'Freddie Mercury', 'freddie.mercury', 'freddie@example.test', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiY6Pz/5N4qpx3vP.f4LyOmdx4Mq2Fy', 'Fixture user', '2026-07-04 10:00:00', '1967-01-24', 'psychiatrist', 'MUDr.', TRUE, 'USER', 0, 3001);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (3001, 1);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (3001, 2);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (3001, 3);

INSERT INTO shift_request_dates_no (shift_request_id, date_no) VALUES (3001, '2026-08-14');
INSERT INTO shift_request_dates_no (shift_request_id, date_no) VALUES (3001, '2026-08-19');

INSERT INTO shift_preference (id, shift_type, priority, weekday_count, weekend_count, no_shift_requested, any_date_selected, shift_request_id)
VALUES (7001, 1, 1, 2, 1, FALSE, FALSE, 3001);
INSERT INTO shift_preference_dates_yes (shift_preference_id, dates_yes) VALUES (7001, '2026-08-01');
INSERT INTO shift_preference_dates_yes (shift_preference_id, dates_yes) VALUES (7001, '2026-08-10');
INSERT INTO shift_preference_dates_yes (shift_preference_id, dates_yes) VALUES (7001, '2026-08-15');

INSERT INTO shift_preference (id, shift_type, priority, weekday_count, weekend_count, no_shift_requested, any_date_selected, shift_request_id)
VALUES (7002, 2, 2, 1, 0, FALSE, TRUE, 3001);

INSERT INTO users (user_id, name, username, email, password, note, creation_date, birthday, profession, title, enabled, role, version, shift_request_id)
VALUES (3002, 'Mick Jagger', 'mick.jagger', 'mick@example.test', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiY6Pz/5N4qpx3vP.f4LyOmdx4Mq2Fy', 'Fixture user without request', '2026-07-05 09:00:00', '1987-06-09', 'psychiatrist', 'Bc.', TRUE, 'USER', 0, NULL);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (3002, 1);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (3002, 2);

SET FOREIGN_KEY_CHECKS = 1;
