SET NAMES utf8mb4;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_task' AND index_name='idx_approval_pending_assignee')=0, 'ALTER TABLE approval_task ADD KEY idx_approval_pending_assignee (status, assigned_user_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_task' AND index_name='idx_approval_pending_role')=0, 'ALTER TABLE approval_task ADD KEY idx_approval_pending_role (status, approver_role, assigned_user_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_task' AND index_name='idx_approval_booking_level')=0, 'ALTER TABLE approval_task ADD KEY idx_approval_booking_level (booking_id, level)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_task' AND index_name='idx_approval_booking_status')=0, 'ALTER TABLE approval_task ADD KEY idx_approval_booking_status (booking_id, status)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_record' AND index_name='idx_approval_record_task')=0, 'ALTER TABLE approval_record ADD KEY idx_approval_record_task (task_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_record' AND index_name='idx_approval_record_booking')=0, 'ALTER TABLE approval_record ADD KEY idx_approval_record_booking (booking_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_record' AND index_name='idx_approval_record_request')=0, 'ALTER TABLE approval_record ADD KEY idx_approval_record_request (request_id, task_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='booking' AND index_name='idx_booking_resource_status_time')=0, 'ALTER TABLE booking ADD KEY idx_booking_resource_status_time (resource_id, status, start_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='booking' AND index_name='idx_booking_status_time')=0, 'ALTER TABLE booking ADD KEY idx_booking_status_time (status, start_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='booking' AND index_name='idx_booking_user_status_time')=0, 'ALTER TABLE booking ADD KEY idx_booking_user_status_time (user_id, status, start_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='booking' AND index_name='idx_booking_user_start')=0, 'ALTER TABLE booking ADD KEY idx_booking_user_start (user_id, start_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='booking' AND index_name='idx_booking_start_time')=0, 'ALTER TABLE booking ADD KEY idx_booking_start_time (start_time)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='booking_slot' AND index_name='idx_slot_booking_released')=0, 'ALTER TABLE booking_slot ADD KEY idx_slot_booking_released (booking_id, released_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='violation_record' AND index_name='idx_violation_user_type_time')=0, 'ALTER TABLE violation_record ADD KEY idx_violation_user_type_time (user_id, violation_type, status, created_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
