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

INSERT INTO users (user_id, name, username, email, password, note, creation_date, birthday, profession, title, enabled, role, shift_request_id)
VALUES (2001, 'Admin Root', 'admin.root', 'admin@example.test', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiY6Pz/5N4qpx3vP.f4LyOmdx4Mq2Fy', 'Test admin', '2026-07-01 08:00:00', '1970-01-01', 'Doctor', 'MUDr.', TRUE, 'ADMIN', NULL);

INSERT INTO users (user_id, name, username, email, password, note, creation_date, birthday, profession, title, enabled, role, shift_request_id)
VALUES (2002, 'Alice Doe', 'alice.doe', 'alice@example.test', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiY6Pz/5N4qpx3vP.f4LyOmdx4Mq2Fy', 'Test user 1', '2026-07-02 09:00:00', '1990-05-05', 'Doctor', '', TRUE, 'USER', NULL);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (2002, 1);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (2002, 2);

INSERT INTO users (user_id, name, username, email, password, note, creation_date, birthday, profession, title, enabled, role, shift_request_id)
VALUES (2003, 'Bob Smith', 'bob.smith', 'bob@example.test', '$2a$10$7EqJtq98hPqEX7fNZaFWoOhiY6Pz/5N4qpx3vP.f4LyOmdx4Mq2Fy', 'Test user 2', '2026-07-03 09:00:00', '1988-06-06', 'psychiatrist', 'Bc.', TRUE, 'USER', NULL);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (2003, 3);
INSERT INTO user_allowed_shift_types (user_id, shift_type) VALUES (2003, 4);

SET FOREIGN_KEY_CHECKS = 1;
