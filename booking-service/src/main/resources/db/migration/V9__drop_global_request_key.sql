SET NAMES utf8mb4;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='booking' AND index_name='uk_client_request_id')>0, 'ALTER TABLE booking DROP KEY uk_client_request_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
