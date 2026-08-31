-- Laboratory equipment booking database structure.
-- Baseline: the live MySQL instance currently used by the services.
-- Unused leftover tables lab_user.users and lab_resource.resources are omitted.
-- Run on a fresh MySQL 8 instance, then load demonstration data with test-data.sql.
SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS lab_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_resource DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_booking DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_approval DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_notification DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_statistics DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS lab_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE lab_user;
CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT NOT NULL AUTO_INCREMENT,
  employee_no VARCHAR(255) NOT NULL,
  username VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(255) NOT NULL,
  email VARCHAR(255) NULL,
  phone VARCHAR(255) NULL,
  status VARCHAR(255) NOT NULL,
  failed_login_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME(3) NULL,
  token_version INT NOT NULL DEFAULT 0,
  last_login_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY username (username),
  UNIQUE KEY employee_no (employee_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(30) NOT NULL,
  name VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS permission (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(100) NOT NULL,
  name VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  PRIMARY KEY (id),
  UNIQUE KEY uk_permission_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  KEY idx_user_role_role (role_id),
  CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES `user` (id),
  CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  KEY idx_role_permission_permission (permission_id),
  CONSTRAINT fk_role_permission_role FOREIGN KEY (role_id) REFERENCES role (id),
  CONSTRAINT fk_role_permission_permission FOREIGN KEY (permission_id) REFERENCES permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS refresh_token (
  id BIGINT NOT NULL AUTO_INCREMENT,
  token_hash VARCHAR(128) NOT NULL,
  user_id BIGINT NOT NULL,
  token_version INT NOT NULL,
  expires_at DATETIME(3) NOT NULL,
  revoked_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY token_hash (token_hash),
  KEY idx_refresh_user (user_id, revoked_at, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_resource;
CREATE TABLE IF NOT EXISTS resource_type (
  id BIGINT NOT NULL AUTO_INCREMENT,
  default_approval_level INT NOT NULL,
  enabled BIT(1) NOT NULL,
  name VARCHAR(255) NOT NULL,
  default_need_checkin TINYINT(1) NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_type_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource (
  id BIGINT NOT NULL AUTO_INCREMENT,
  type_id BIGINT NOT NULL,
  name VARCHAR(255) NOT NULL,
  location VARCHAR(255) NULL,
  capacity INT NOT NULL,
  status VARCHAR(255) NULL,
  description VARCHAR(255) NULL,
  owner_user_id BIGINT NULL,
  image_url VARCHAR(255) NULL,
  need_checkin TINYINT(1) NOT NULL DEFAULT 1,
  max_duration_minutes INT NOT NULL DEFAULT 120,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  approval_required_override TINYINT(1) NULL,
  approval_level_override INT NULL,
  slot_minutes INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_type_name (type_id, name),
  KEY idx_resource_status_type (status, type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_schedule (
  id BIGINT NOT NULL AUTO_INCREMENT,
  close_time TIME(6) NULL,
  enabled BIT(1) NOT NULL,
  max_duration_minutes INT NOT NULL,
  open_time TIME(6) NULL,
  resource_id BIGINT NULL,
  slot_minutes INT NOT NULL,
  weekday INT NOT NULL,
  effective_from DATE NULL,
  effective_to DATE NULL,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_closure (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NULL,
  created_by BIGINT NULL,
  end_time DATETIME(6) NULL,
  reason VARCHAR(255) NULL,
  resource_id BIGINT NULL,
  start_time DATETIME(6) NULL,
  status VARCHAR(255) NULL,
  handled_booking_policy VARCHAR(255) NULL,
  updated_at DATETIME(3) NULL,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_manager (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  manager_type VARCHAR(255) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  scope_type VARCHAR(255) NULL,
  scope_value VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_manager (resource_id, user_id, manager_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS asset_category (
  id BIGINT NOT NULL AUTO_INCREMENT,
  created_at DATETIME(6) NULL,
  description VARCHAR(255) NULL,
  enabled BIT(1) NOT NULL,
  high_value BIT(1) NOT NULL,
  name VARCHAR(255) NOT NULL,
  serialized BIT(1) NOT NULL,
  updated_at DATETIME(6) NULL,
  version INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_asset_category_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS asset (
  id BIGINT NOT NULL AUTO_INCREMENT,
  asset_no VARCHAR(255) NOT NULL,
  brand VARCHAR(255) NULL,
  category_id BIGINT NOT NULL,
  created_at DATETIME(6) NULL,
  custodian_user_id BIGINT NULL,
  deleted BIT(1) NOT NULL,
  location VARCHAR(255) NULL,
  model VARCHAR(255) NULL,
  name VARCHAR(255) NOT NULL,
  original_cost DECIMAL(38,2) NULL,
  purchase_date DATE NULL,
  remark VARCHAR(255) NULL,
  resource_id BIGINT NULL,
  serial_no VARCHAR(255) NULL,
  specification VARCHAR(255) NULL,
  status VARCHAR(255) NULL,
  updated_at DATETIME(6) NULL,
  version INT NOT NULL,
  warranty_until DATE NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_asset_no (asset_no),
  UNIQUE KEY uk_asset_serial_no (serial_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS asset_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  asset_id BIGINT NOT NULL,
  created_at DATETIME(6) NULL,
  from_status VARCHAR(255) NULL,
  operator_id BIGINT NULL,
  reason VARCHAR(255) NULL,
  to_status VARCHAR(255) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS maintenance_ticket (
  id BIGINT NOT NULL AUTO_INCREMENT,
  actual_cost DECIMAL(38,2) NULL,
  asset_id BIGINT NULL,
  resource_id BIGINT NULL,
  location_snapshot VARCHAR(255) NULL,
  asset_clue VARCHAR(255) NULL,
  assigned_to VARCHAR(30) NULL,
  closed_at DATETIME(6) NULL,
  created_at DATETIME(6) NULL,
  description VARCHAR(2000) NOT NULL,
  estimated_cost DECIMAL(38,2) NULL,
  previous_asset_status VARCHAR(255) NULL,
  processed_at DATETIME(6) NULL,
  processed_by BIGINT NULL,
  report_type VARCHAR(255) NULL,
  reported_at DATETIME(6) NULL,
  reported_by BIGINT NULL,
  resolution VARCHAR(255) NULL,
  severity VARCHAR(255) NULL,
  status VARCHAR(255) NULL,
  ticket_no VARCHAR(255) NOT NULL,
  updated_at DATETIME(6) NULL,
  version INT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_ticket_no (ticket_no),
  KEY idx_maintenance_resource (resource_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_booking;
CREATE TABLE IF NOT EXISTS booking (
  id BIGINT NOT NULL AUTO_INCREMENT,
  applicant_name_snapshot VARCHAR(255) NULL,
  approval_level_snapshot INT NOT NULL,
  booking_no VARCHAR(255) NOT NULL,
  cancel_reason VARCHAR(255) NULL,
  canceled_at DATETIME(6) NULL,
  checkin_at DATETIME(6) NULL,
  client_request_id VARCHAR(255) NOT NULL,
  completed_at DATETIME(6) NULL,
  created_at DATETIME(6) NULL,
  end_time DATETIME(6) NULL,
  need_checkin_snapshot BIT(1) NOT NULL,
  participants INT NOT NULL,
  purpose VARCHAR(255) NULL,
  resource_id BIGINT NULL,
  resource_name_snapshot VARCHAR(255) NULL,
  slot_minutes_snapshot INT NOT NULL,
  start_time DATETIME(6) NULL,
  status VARCHAR(255) NULL,
  updated_at DATETIME(6) NULL,
  user_id BIGINT NULL,
  version INT NOT NULL,
  approval_flow_version INT NULL,
  approval_deadline DATETIME(3) NULL,
  forced TINYINT(1) NOT NULL DEFAULT 0,
  force_reason VARCHAR(500) NULL,
  forced_by BIGINT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  reject_reason VARCHAR(500) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_booking_no (booking_no),
  UNIQUE KEY uk_client_request_id (client_request_id),
  UNIQUE KEY uk_booking_user_request (user_id, client_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS booking_quota_lock (
  user_id BIGINT NOT NULL,
  PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS booking_slot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_id BIGINT NULL,
  created_at DATETIME(6) NULL,
  resource_id BIGINT NULL,
  slot_start DATETIME(6) NULL,
  released_at DATETIME(3) NULL,
  release_reason VARCHAR(255) NULL,
  active_slot_key VARCHAR(128) GENERATED ALWAYS AS (
    CASE WHEN released_at IS NULL
      THEN CONCAT(resource_id, ':', DATE_FORMAT(slot_start, '%Y-%m-%d %H:%i:%s.%f'))
      ELSE NULL END
  ) STORED,
  PRIMARY KEY (id),
  UNIQUE KEY uk_resource_slot_active (active_slot_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS booking_participant (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  name_snapshot VARCHAR(50) NOT NULL,
  phone_snapshot VARCHAR(30) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS booking_status_history (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  from_status VARCHAR(255) NULL,
  to_status VARCHAR(255) NULL,
  operator_id BIGINT NULL,
  reason VARCHAR(255) NULL,
  request_id VARCHAR(255) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_booking_history (booking_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS violation_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  violation_type VARCHAR(255) NULL,
  status VARCHAR(255) NULL,
  comment VARCHAR(255) NULL,
  processed_by BIGINT NULL,
  processed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_violation_booking_type (booking_id, violation_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS user_restriction (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  restricted_until DATETIME(3) NOT NULL,
  reason VARCHAR(255) NULL,
  source_violation_count INT NOT NULL,
  status VARCHAR(255) NULL,
  created_by BIGINT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_restriction_user_status (user_id, status, restricted_until)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS idempotency_record (
  id BIGINT NOT NULL AUTO_INCREMENT,
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
  PRIMARY KEY (id),
  UNIQUE KEY uk_idempotency (operator_id, idempotency_key, request_uri)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT NOT NULL AUTO_INCREMENT,
  event_id VARCHAR(255) NOT NULL,
  event_type VARCHAR(255) NULL,
  aggregate_type VARCHAR(255) NULL,
  aggregate_id BIGINT NOT NULL,
  payload JSON NULL,
  status VARCHAR(255) NULL,
  retry_count INT NOT NULL DEFAULT 0,
  next_retry_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  last_error VARCHAR(255) NULL,
  sent_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY event_id (event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_approval;
CREATE TABLE IF NOT EXISTS approval_flow (
  id BIGINT NOT NULL AUTO_INCREMENT,
  resource_type_id BIGINT NOT NULL,
  version INT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  created_by BIGINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_approval_flow (resource_type_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS approval_node (
  id BIGINT NOT NULL AUTO_INCREMENT,
  flow_id BIGINT NOT NULL,
  level INT NOT NULL,
  approver_role VARCHAR(255) NULL,
  approver_scope VARCHAR(100) NULL,
  sequence_no INT NOT NULL DEFAULT 1,
  scope_type VARCHAR(255) NULL,
  scope_value VARCHAR(255) NULL,
  approval_rule VARCHAR(255) NULL,
  quorum_count INT NULL,
  deadline_minutes INT NOT NULL DEFAULT 1440,
  PRIMARY KEY (id),
  UNIQUE KEY uk_approval_node (flow_id, level, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
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
CREATE TABLE IF NOT EXISTS approval_task_assignee (
  id BIGINT NOT NULL AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_task_assignee (task_id, user_id)
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

USE lab_notification;
CREATE TABLE IF NOT EXISTS announcement (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  publisher_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  published_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(255) NULL,
  title VARCHAR(255) NULL,
  content VARCHAR(255) NULL,
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  read_at DATETIME(3) NULL,
  PRIMARY KEY (id),
  KEY idx_notification_user_read_time (user_id, is_read, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS notification_delivery (
  id BIGINT NOT NULL AUTO_INCREMENT,
  notification_id BIGINT NOT NULL,
  channel VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000) NULL,
  sent_at DATETIME(3) NULL,
  next_retry_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  KEY idx_delivery_dispatch (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS notification_template (
  id BIGINT NOT NULL AUTO_INCREMENT,
  template_code VARCHAR(50) NOT NULL,
  channel VARCHAR(20) NOT NULL,
  title_template VARCHAR(200) NOT NULL,
  content_template TEXT NOT NULL,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY template_code (template_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_statistics;
CREATE TABLE IF NOT EXISTS statistics_event_offset (
  consumer_name VARCHAR(100) NOT NULL,
  last_event_id VARCHAR(64) NULL,
  last_created_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (consumer_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS statistics_snapshot (
  id BIGINT NOT NULL AUTO_INCREMENT,
  metric_type VARCHAR(255) NULL,
  resource_id BIGINT NULL,
  user_id BIGINT NULL,
  period_start DATETIME(3) NOT NULL,
  period_end DATETIME(3) NOT NULL,
  numerator DECIMAL(38,2) NULL,
  denominator DECIMAL(38,2) NULL,
  metric_value DECIMAL(38,2) NULL,
  calculated_until DATETIME(3) NOT NULL,
  data_version BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (id),
  UNIQUE KEY uk_statistics_metric (metric_type, resource_id, user_id, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_system;
CREATE TABLE IF NOT EXISTS data_dictionary (
  id BIGINT NOT NULL AUTO_INCREMENT,
  dict_type VARCHAR(50) NOT NULL,
  dict_code VARCHAR(50) NOT NULL,
  dict_label VARCHAR(100) NOT NULL,
  sort_no INT NOT NULL DEFAULT 0,
  enabled TINYINT(1) NOT NULL DEFAULT 1,
  version INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_dictionary (dict_type, dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  operator_id BIGINT NOT NULL,
  operation_type VARCHAR(255) NULL,
  target_type VARCHAR(255) NULL,
  target_id BIGINT NULL,
  request_id VARCHAR(255) NULL,
  ip VARCHAR(255) NULL,
  detail JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  result VARCHAR(255) NULL,
  reason VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY idx_log_operator_time (operator_id, created_at),
  KEY idx_log_target_time (target_type, target_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  config_key VARCHAR(255) NULL,
  config_value VARCHAR(255) NULL,
  description VARCHAR(255) NULL,
  updated_by BIGINT NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  value_type VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_user;
INSERT IGNORE INTO role(code, name, status) VALUES
  ('STUDENT', 'Student', 'ACTIVE'),
  ('TEACHER', 'Teacher', 'ACTIVE'),
  ('LAB_ADMIN', 'Lab administrator', 'ACTIVE'),
  ('SYSTEM_ADMIN', 'System administrator', 'ACTIVE');
