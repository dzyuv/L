SET NAMES utf8mb4;

ALTER TABLE system_config
  MODIFY config_value VARCHAR(500) NULL;

ALTER TABLE operation_log
  MODIFY reason VARCHAR(500) NULL;
