SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS approval_task (
  id BIGINT NOT NULL AUTO_INCREMENT,
  approver_role VARCHAR(255) NULL,
  assigned_user_id BIGINT NULL,
  booking_id BIGINT NULL,
  comment VARCHAR(255) NULL,
  completed_at DATETIME(6) NULL,
  created_at DATETIME(6) NULL,
  deadline DATETIME(6) NULL,
  level INT NOT NULL,
  status VARCHAR(255) NULL,
  flow_version INT NOT NULL DEFAULT 1,
  sequence_no INT NOT NULL DEFAULT 1,
  scope_type VARCHAR(30) NOT NULL DEFAULT 'RESOURCE',
  scope_value VARCHAR(100) NOT NULL DEFAULT '',
  approval_rule VARCHAR(20) NOT NULL DEFAULT 'ANY_ONE',
  version INT NOT NULL,
  applicant_user_id BIGINT NULL,
  applicant_name VARCHAR(255) NULL,
  end_time DATETIME(6) NULL,
  resource_id BIGINT NULL,
  resource_name VARCHAR(255) NULL,
  start_time DATETIME(6) NULL,
  resource_type_id BIGINT NULL,
  total_levels INT NOT NULL DEFAULT 1,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS approval_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  booking_id BIGINT NOT NULL,
  approver_id BIGINT NOT NULL,
  result VARCHAR(255) NULL,
  comment VARCHAR(255) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  request_id VARCHAR(255) NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- One-time data migration from the former lab_approval database.
SET @has_task = (SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema = 'lab_approval' AND table_name = 'approval_task');
SET @sql = IF(@has_task > 0,
  'INSERT IGNORE INTO approval_task SELECT * FROM lab_approval.approval_task',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_record = (SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema = 'lab_approval' AND table_name = 'approval_record');
SET @sql = IF(@has_record > 0,
  'INSERT IGNORE INTO approval_record SELECT * FROM lab_approval.approval_record',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- The approval workflow is now local to booking-service; the outbox is no longer used.
DROP TABLE IF EXISTS booking_outbox;
