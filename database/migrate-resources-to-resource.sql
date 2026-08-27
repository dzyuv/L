-- Optional compatibility migration for databases created by the MVP plural
-- table. Run this only when lab_resource.resources exists, after schema.sql.
SET NAMES utf8mb4;
INSERT INTO lab_resource.resource (id, type_id, name, location, capacity, status, description, owner_user_id, image_url, approval_required_override, approval_level_override, need_checkin, max_duration_minutes)
SELECT id, type_id, name, location, capacity, status, description, owner_user_id, image_url, NULL, NULL, TRUE, 120
FROM lab_resource.resources
ON DUPLICATE KEY UPDATE name=VALUES(name), location=VALUES(location), capacity=VALUES(capacity), status=VALUES(status), description=VALUES(description);
