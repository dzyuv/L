SET NAMES utf8mb4;

DELETE duplicate_row FROM approval_record duplicate_row
JOIN approval_record kept_row
  ON duplicate_row.request_id = kept_row.request_id
 AND duplicate_row.task_id = kept_row.task_id
 AND duplicate_row.id > kept_row.id
WHERE duplicate_row.request_id IS NOT NULL;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_record' AND index_name='idx_approval_record_request')>0, 'ALTER TABLE approval_record DROP KEY idx_approval_record_request', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='approval_record' AND index_name='uk_approval_record_request')=0, 'ALTER TABLE approval_record ADD UNIQUE KEY uk_approval_record_request (request_id, task_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
