-- Optional compatibility migration for databases created by the earlier MVP.
-- Run this only when lab_user.users exists, after schema.sql. It does not drop
-- the legacy table and preserves existing ids and password hashes.
SET NAMES utf8mb4;
INSERT INTO lab_user.`user` (id, employee_no, username, password_hash, real_name, email, phone, status, failed_login_count, locked_until, token_version, last_login_at, created_at, updated_at, version, deleted)
SELECT id, COALESCE(NULLIF(employee_no, ''), username), username, password_hash, real_name, email, phone, status, failed_login_count, locked_until, token_version, last_login_at, created_at, updated_at, version, deleted
FROM lab_user.users
ON DUPLICATE KEY UPDATE username=VALUES(username), real_name=VALUES(real_name), password_hash=VALUES(password_hash), email=VALUES(email), phone=VALUES(phone), status=VALUES(status);
INSERT IGNORE INTO lab_user.user_role(user_id,role_id) SELECT u.id,r.id FROM lab_user.`user` u JOIN lab_user.role r ON r.code='STUDENT';
