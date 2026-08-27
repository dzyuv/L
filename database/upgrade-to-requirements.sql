-- Upgrade an existing MVP installation in place.
-- Run after schema.sql. This script never drops tables or data.
SET NAMES utf8mb4;

USE lab_system;

DROP PROCEDURE IF EXISTS add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE add_column_if_missing(
  IN p_schema VARCHAR(64), IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition TEXT
)
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = p_schema AND table_name = p_table AND column_name = p_column
  ) THEN
    SET @upgrade_sql = CONCAT('ALTER TABLE `', p_schema, '`.`', p_table,
                              '` ADD COLUMN `', p_column, '` ', p_definition);
    PREPARE upgrade_stmt FROM @upgrade_sql;
    EXECUTE upgrade_stmt;
    DEALLOCATE PREPARE upgrade_stmt;
  END IF;
END$$
DELIMITER ;

USE lab_user;
UPDATE `user` SET employee_no = username WHERE employee_no IS NULL OR employee_no = '';
ALTER TABLE `user` MODIFY employee_no VARCHAR(50) NOT NULL;
SET @legacy_user_role_fk = (
  SELECT IF(COUNT(*) > 0,
    CONCAT('ALTER TABLE lab_user.user_role DROP FOREIGN KEY `', MAX(constraint_name), '`'),
    'SELECT 1')
  FROM information_schema.key_column_usage
  WHERE table_schema = 'lab_user'
    AND table_name = 'user_role'
    AND referenced_table_name = 'users'
);
PREPARE legacy_user_role_stmt FROM @legacy_user_role_fk;
EXECUTE legacy_user_role_stmt;
DEALLOCATE PREPARE legacy_user_role_stmt;
CALL lab_system.add_column_if_missing('lab_user', 'permission', 'status', "VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'");
CREATE TABLE IF NOT EXISTS refresh_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  token_hash VARCHAR(128) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  token_version INT NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  revoked_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_refresh_user(user_id, revoked_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_resource;
CALL lab_system.add_column_if_missing('lab_resource', 'resource_type', 'default_need_checkin', 'BOOLEAN NOT NULL DEFAULT TRUE');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_type', 'version', 'INT NOT NULL DEFAULT 0');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_type', 'deleted', 'BOOLEAN NOT NULL DEFAULT FALSE');
CALL lab_system.add_column_if_missing('lab_resource', 'resource', 'approval_required_override', 'BOOLEAN NULL');
CALL lab_system.add_column_if_missing('lab_resource', 'resource', 'approval_level_override', 'TINYINT NULL');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_schedule', 'effective_from', 'DATE NULL');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_schedule', 'effective_to', 'DATE NULL');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_schedule', 'version', 'INT NOT NULL DEFAULT 0');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_closure', 'handled_booking_policy', 'VARCHAR(30) NULL');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_closure', 'created_by', 'BIGINT NULL');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_closure', 'updated_at', 'DATETIME(3) NULL');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_closure', 'version', 'INT NOT NULL DEFAULT 0');
CALL lab_system.add_column_if_missing('lab_resource', 'resource_manager', 'scope_type', "VARCHAR(30) NOT NULL DEFAULT 'RESOURCE'");
CALL lab_system.add_column_if_missing('lab_resource', 'resource_manager', 'scope_value', "VARCHAR(100) NOT NULL DEFAULT ''");
CREATE TABLE IF NOT EXISTS lab_resource.asset_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(100) NOT NULL UNIQUE,
  serialized BOOLEAN NOT NULL DEFAULT TRUE, high_value BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE, description VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS lab_resource.asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, asset_no VARCHAR(50) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL, resource_id BIGINT NULL, serial_no VARCHAR(100) NULL UNIQUE,
  brand VARCHAR(100) NULL, model VARCHAR(100) NULL, specification VARCHAR(500) NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK', location VARCHAR(200) NULL, custodian_user_id BIGINT NULL,
  purchase_date DATE NULL, warranty_until DATE NULL, original_cost DECIMAL(14,2) NULL, remark VARCHAR(1000) NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), version INT NOT NULL DEFAULT 0,
  KEY idx_asset_status_category(status, category_id), KEY idx_asset_resource(resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS lab_resource.asset_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, asset_id BIGINT NOT NULL, from_status VARCHAR(30) NULL, to_status VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NULL, operator_id BIGINT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), KEY idx_asset_history(asset_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS lab_resource.maintenance_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, ticket_no VARCHAR(32) NOT NULL UNIQUE, asset_id BIGINT NOT NULL, reported_by BIGINT NULL,
  previous_asset_status VARCHAR(30) NULL, report_type VARCHAR(30) NOT NULL DEFAULT 'MALFUNCTION', severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM', description VARCHAR(2000) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'REPORTED', assigned_to BIGINT NULL, estimated_cost DECIMAL(14,2) NULL, actual_cost DECIMAL(14,2) NULL, resolution VARCHAR(2000) NULL,
  processed_by BIGINT NULL, reported_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), processed_at DATETIME(3) NULL, closed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), version INT NOT NULL DEFAULT 0,
  KEY idx_maintenance_status(status, created_at), KEY idx_maintenance_asset(asset_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_booking;
CALL lab_system.add_column_if_missing('lab_booking', 'booking', 'slot_minutes_snapshot', 'INT NOT NULL DEFAULT 30');
CALL lab_system.add_column_if_missing('lab_booking', 'booking', 'approval_flow_version', 'INT NULL');
CALL lab_system.add_column_if_missing('lab_booking', 'booking', 'approval_deadline', 'DATETIME(3) NULL');
CALL lab_system.add_column_if_missing('lab_booking', 'booking', 'forced', 'BOOLEAN NOT NULL DEFAULT FALSE');
CALL lab_system.add_column_if_missing('lab_booking', 'booking', 'force_reason', 'VARCHAR(500) NULL');
CALL lab_system.add_column_if_missing('lab_booking', 'booking', 'forced_by', 'BIGINT NULL');
CALL lab_system.add_column_if_missing('lab_booking', 'booking', 'deleted', 'BOOLEAN NOT NULL DEFAULT FALSE');
SET @legacy_booking_idempotency_index = (
  SELECT MAX(index_name)
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema='lab_booking' AND table_name='booking' AND non_unique=0 AND index_name <> 'PRIMARY'
    GROUP BY index_name
    HAVING COUNT(*)=1 AND SUM(column_name='client_request_id')=1
  ) AS legacy_booking_idempotency_indexes
);
SET @drop_booking_idempotency_index = IF(@legacy_booking_idempotency_index IS NULL, 'SELECT 1', CONCAT('ALTER TABLE lab_booking.booking DROP INDEX `', @legacy_booking_idempotency_index, '`'));
PREPARE drop_booking_idempotency_stmt FROM @drop_booking_idempotency_index; EXECUTE drop_booking_idempotency_stmt; DEALLOCATE PREPARE drop_booking_idempotency_stmt;
SET @add_booking_idempotency_index = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE lab_booking.booking ADD UNIQUE KEY uk_booking_user_request(user_id, client_request_id)', 'SELECT 1') FROM information_schema.statistics WHERE table_schema='lab_booking' AND table_name='booking' AND index_name='uk_booking_user_request');
PREPARE add_booking_idempotency_stmt FROM @add_booking_idempotency_index; EXECUTE add_booking_idempotency_stmt; DEALLOCATE PREPARE add_booking_idempotency_stmt;
CALL lab_system.add_column_if_missing('lab_booking', 'booking_slot', 'released_at', 'DATETIME(3) NULL');
CALL lab_system.add_column_if_missing('lab_booking', 'booking_slot', 'release_reason', 'VARCHAR(100) NULL');
CALL lab_system.add_column_if_missing('lab_booking', 'booking_slot', 'active_slot_key', "VARCHAR(128) GENERATED ALWAYS AS (CASE WHEN released_at IS NULL THEN CONCAT(resource_id, ':', DATE_FORMAT(slot_start, '%Y-%m-%d %H:%i:%s.%f')) ELSE NULL END) STORED");
SET @legacy_slot_index = (
  SELECT MAX(index_name)
  FROM (
    SELECT index_name
    FROM information_schema.statistics
    WHERE table_schema='lab_booking'
      AND table_name='booking_slot'
      AND non_unique=0
      AND index_name <> 'PRIMARY'
    GROUP BY index_name
    HAVING COUNT(*)=2
       AND SUM(column_name='resource_id')=1
       AND SUM(column_name='slot_start')=1
  ) AS legacy_slot_indexes
);
SET @drop_slot_index = IF(@legacy_slot_index IS NULL, 'SELECT 1', CONCAT('ALTER TABLE lab_booking.booking_slot DROP INDEX `', @legacy_slot_index, '`'));
PREPARE drop_slot_stmt FROM @drop_slot_index; EXECUTE drop_slot_stmt; DEALLOCATE PREPARE drop_slot_stmt;
SET @add_slot_index = (SELECT IF(COUNT(*) = 0, 'ALTER TABLE lab_booking.booking_slot ADD UNIQUE KEY uk_resource_slot_active(active_slot_key)', 'SELECT 1') FROM information_schema.statistics WHERE table_schema='lab_booking' AND table_name='booking_slot' AND index_name='uk_resource_slot_active');
PREPARE add_slot_stmt FROM @add_slot_index; EXECUTE add_slot_stmt; DEALLOCATE PREPARE add_slot_stmt;
CALL lab_system.add_column_if_missing('lab_booking', 'booking_participant', 'created_at', 'DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)');
CALL lab_system.add_column_if_missing('lab_booking', 'violation_record', 'processed_at', 'DATETIME(3) NULL');
CALL lab_system.add_column_if_missing('lab_booking', 'outbox_event', 'last_error', 'VARCHAR(1000) NULL');
CALL lab_system.add_column_if_missing('lab_booking', 'outbox_event', 'sent_at', 'DATETIME(3) NULL');
CREATE TABLE IF NOT EXISTS booking_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  from_status VARCHAR(30) NULL,
  to_status VARCHAR(30) NOT NULL,
  operator_id BIGINT NULL,
  reason VARCHAR(500) NULL,
  request_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_booking_history(booking_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS user_restriction (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  restricted_until DATETIME(3) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  source_violation_count INT NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_by BIGINT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  KEY idx_restriction_user_status(user_id, status, restricted_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS idempotency_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  idempotency_key VARCHAR(64) NOT NULL,
  operator_id BIGINT NOT NULL,
  request_uri VARCHAR(200) NOT NULL,
  request_hash VARCHAR(64) NOT NULL,
  response_status INT NULL,
  response_body JSON NULL,
  status VARCHAR(20) NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_idempotency(operator_id, idempotency_key, request_uri)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_notification;
CREATE TABLE IF NOT EXISTS notification_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  template_code VARCHAR(50) NOT NULL UNIQUE,
  channel VARCHAR(20) NOT NULL,
  title_template VARCHAR(200) NOT NULL,
  content_template TEXT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS notification_delivery (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  notification_id BIGINT NOT NULL,
  channel VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000) NULL,
  sent_at DATETIME(3) NULL,
  next_retry_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_delivery_dispatch(status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_statistics;
CREATE TABLE IF NOT EXISTS statistics_event_offset (
  consumer_name VARCHAR(100) PRIMARY KEY,
  last_event_id VARCHAR(64) NULL,
  last_created_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_approval;
CALL lab_system.add_column_if_missing('lab_approval', 'approval_flow', 'created_by', 'BIGINT NOT NULL DEFAULT 0');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_node', 'sequence_no', 'INT NOT NULL DEFAULT 1');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_node', 'scope_type', "VARCHAR(30) NOT NULL DEFAULT 'RESOURCE'");
CALL lab_system.add_column_if_missing('lab_approval', 'approval_node', 'scope_value', "VARCHAR(100) NOT NULL DEFAULT ''");
CALL lab_system.add_column_if_missing('lab_approval', 'approval_node', 'approval_rule', "VARCHAR(20) NOT NULL DEFAULT 'ANY_ONE'");
CALL lab_system.add_column_if_missing('lab_approval', 'approval_node', 'quorum_count', 'INT NULL');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_node', 'deadline_minutes', 'INT NOT NULL DEFAULT 1440');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'flow_version', 'INT NOT NULL DEFAULT 1');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'sequence_no', 'INT NOT NULL DEFAULT 1');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'scope_type', "VARCHAR(30) NOT NULL DEFAULT 'RESOURCE'");
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'scope_value', "VARCHAR(100) NOT NULL DEFAULT ''");
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'approval_rule', "VARCHAR(20) NOT NULL DEFAULT 'ANY_ONE'");
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'applicant_user_id', 'BIGINT NULL');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'applicant_name', 'VARCHAR(50) NULL');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'resource_id', 'BIGINT NULL');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'resource_name', 'VARCHAR(100) NULL');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'start_time', 'DATETIME(3) NULL');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_task', 'end_time', 'DATETIME(3) NULL');
CALL lab_system.add_column_if_missing('lab_approval', 'approval_record', 'request_id', 'VARCHAR(64) NULL');
CREATE TABLE IF NOT EXISTS approval_task_assignee (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_task_assignee(task_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_system;
CREATE TABLE IF NOT EXISTS data_dictionary (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  dict_type VARCHAR(50) NOT NULL,
  dict_code VARCHAR(50) NOT NULL,
  dict_label VARCHAR(100) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  version INT NOT NULL DEFAULT 0,
  UNIQUE KEY uk_dictionary(dict_type, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CALL lab_system.add_column_if_missing('lab_system', 'system_config', 'value_type', "VARCHAR(20) NOT NULL DEFAULT 'STRING'");
CALL lab_system.add_column_if_missing('lab_system', 'operation_log', 'result', "VARCHAR(20) NOT NULL DEFAULT 'SUCCESS'");
CALL lab_system.add_column_if_missing('lab_system', 'operation_log', 'reason', 'VARCHAR(500) NULL');

DROP PROCEDURE IF EXISTS add_column_if_missing;

