-- Laboratory equipment booking databases.
-- Requirement baseline: 需求分析.docx (version 2.0).
-- This script is safe for a fresh installation and is repeatable.
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
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  employee_no VARCHAR(50) NOT NULL UNIQUE,
  username VARCHAR(50) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  real_name VARCHAR(50) NOT NULL,
  email VARCHAR(100) NULL,
  phone VARCHAR(30) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  failed_login_count INT NOT NULL DEFAULT 0,
  locked_until DATETIME(3) NULL,
  token_version INT NOT NULL DEFAULT 0,
  last_login_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS role (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(30) NOT NULL UNIQUE,
  name VARCHAR(50) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(100) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS role_permission (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
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
CREATE TABLE IF NOT EXISTS resource_type (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL UNIQUE,
  default_approval_level TINYINT NOT NULL DEFAULT 1,
  default_need_checkin BOOLEAN NOT NULL DEFAULT TRUE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  type_id BIGINT NOT NULL,
  name VARCHAR(100) NOT NULL,
  location VARCHAR(200) NULL,
  capacity INT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  description VARCHAR(1000) NULL,
  owner_user_id BIGINT NULL,
  image_url VARCHAR(500) NULL,
  approval_required_override BOOLEAN NULL,
  approval_level_override TINYINT NULL,
  need_checkin BOOLEAN NOT NULL DEFAULT TRUE,
  max_duration_minutes INT NOT NULL DEFAULT 120,
  slot_minutes INT NOT NULL DEFAULT 30,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  UNIQUE KEY uk_resource_type_name(type_id, name),
  KEY idx_resource_status_type(status, type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_schedule (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  weekday TINYINT NOT NULL,
  open_time TIME NOT NULL,
  close_time TIME NOT NULL,
  max_duration_minutes INT NOT NULL,
  slot_minutes INT NOT NULL DEFAULT 30,
  effective_from DATE NULL,
  effective_to DATE NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  KEY idx_schedule_resource_day(resource_id, weekday, enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_closure (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  start_time DATETIME(3) NOT NULL,
  end_time DATETIME(3) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PLANNED',
  handled_booking_policy VARCHAR(30) NULL,
  created_by BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  KEY idx_closure_resource_time(resource_id, status, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS resource_manager (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  manager_type VARCHAR(30) NOT NULL,
  scope_type VARCHAR(30) NOT NULL DEFAULT 'RESOURCE',
  scope_value VARCHAR(100) NOT NULL DEFAULT '',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_resource_manager(resource_id, user_id, manager_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS asset_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL UNIQUE,
  serialized BOOLEAN NOT NULL DEFAULT TRUE,
  high_value BOOLEAN NOT NULL DEFAULT FALSE,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  description VARCHAR(500) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_no VARCHAR(50) NOT NULL UNIQUE,
  name VARCHAR(100) NOT NULL,
  category_id BIGINT NOT NULL,
  resource_id BIGINT NULL,
  serial_no VARCHAR(100) NULL UNIQUE,
  brand VARCHAR(100) NULL,
  model VARCHAR(100) NULL,
  specification VARCHAR(500) NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'IN_STOCK',
  location VARCHAR(200) NULL,
  custodian_user_id BIGINT NULL,
  purchase_date DATE NULL,
  warranty_until DATE NULL,
  original_cost DECIMAL(14,2) NULL,
  remark VARCHAR(1000) NULL,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  KEY idx_asset_status_category(status, category_id),
  KEY idx_asset_resource(resource_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS asset_status_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_id BIGINT NULL,
  resource_id BIGINT NULL,
  location_snapshot VARCHAR(200) NULL,
  asset_clue VARCHAR(500) NULL,
  from_status VARCHAR(30) NULL,
  to_status VARCHAR(30) NOT NULL,
  reason VARCHAR(500) NULL,
  operator_id BIGINT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_asset_history(asset_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS maintenance_ticket (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  ticket_no VARCHAR(32) NOT NULL UNIQUE,
  asset_id BIGINT NULL,
  resource_id BIGINT NULL,
  location_snapshot VARCHAR(200) NULL,
  asset_clue VARCHAR(500) NULL,
  reported_by BIGINT NULL,
  previous_asset_status VARCHAR(30) NULL,
  report_type VARCHAR(30) NOT NULL DEFAULT 'MALFUNCTION',
  severity VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
  description VARCHAR(2000) NOT NULL,
  status VARCHAR(30) NOT NULL DEFAULT 'REPORTED',
  assigned_to BIGINT NULL,
  estimated_cost DECIMAL(14,2) NULL,
  actual_cost DECIMAL(14,2) NULL,
  resolution VARCHAR(2000) NULL,
  processed_by BIGINT NULL,
  reported_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  processed_at DATETIME(3) NULL,
  closed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  KEY idx_maintenance_status(status, created_at),
  KEY idx_maintenance_asset(asset_id, status),
  KEY idx_maintenance_resource(resource_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_booking;
CREATE TABLE IF NOT EXISTS booking (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_no VARCHAR(32) NOT NULL UNIQUE,
  user_id BIGINT NOT NULL,
  resource_id BIGINT NOT NULL,
  applicant_name_snapshot VARCHAR(50) NOT NULL,
  resource_name_snapshot VARCHAR(100) NOT NULL,
  start_time DATETIME(3) NOT NULL,
  end_time DATETIME(3) NOT NULL,
  slot_minutes_snapshot INT NOT NULL DEFAULT 30,
  purpose VARCHAR(500) NOT NULL,
  participants INT NOT NULL,
  status VARCHAR(30) NOT NULL,
  approval_level_snapshot TINYINT NOT NULL,
  approval_flow_version INT NULL,
  approval_deadline DATETIME(3) NULL,
  need_checkin_snapshot BOOLEAN NOT NULL DEFAULT TRUE,
  checkin_at DATETIME(3) NULL,
  completed_at DATETIME(3) NULL,
  canceled_at DATETIME(3) NULL,
  cancel_reason VARCHAR(500) NULL,
  forced BOOLEAN NOT NULL DEFAULT FALSE,
  force_reason VARCHAR(500) NULL,
  forced_by BIGINT NULL,
  client_request_id VARCHAR(64) NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  KEY idx_booking_user_status_time(user_id, status, start_time),
  KEY idx_booking_resource_status_time(resource_id, status, start_time, end_time),
  KEY idx_booking_approval_deadline(status, approval_deadline)
  ,UNIQUE KEY uk_booking_user_request(user_id, client_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS booking_quota_lock (
  user_id BIGINT PRIMARY KEY
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS booking_slot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_id BIGINT NOT NULL,
  booking_id BIGINT NOT NULL,
  slot_start DATETIME(3) NOT NULL,
  released_at DATETIME(3) NULL,
  release_reason VARCHAR(100) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  active_slot_key VARCHAR(128) GENERATED ALWAYS AS (
    CASE WHEN released_at IS NULL
      THEN CONCAT(resource_id, ':', DATE_FORMAT(slot_start, '%Y-%m-%d %H:%i:%s.%f'))
      ELSE NULL END
  ) STORED,
  UNIQUE KEY uk_resource_slot_active(active_slot_key),
  KEY idx_slot_booking(booking_id),
  KEY idx_slot_active(resource_id, slot_start, released_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS booking_participant (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  user_id BIGINT NULL,
  name_snapshot VARCHAR(50) NOT NULL,
  phone_snapshot VARCHAR(30) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_participant_booking(booking_id),
  KEY idx_participant_user(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
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
CREATE TABLE IF NOT EXISTS violation_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  violation_type VARCHAR(30) NOT NULL,
  status VARCHAR(20) NOT NULL,
  comment VARCHAR(500) NULL,
  processed_by BIGINT NULL,
  processed_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_violation_booking_type(booking_id, violation_type),
  KEY idx_violation_user_time(user_id, created_at)
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
CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL UNIQUE,
  event_type VARCHAR(100) NOT NULL,
  aggregate_type VARCHAR(50) NOT NULL,
  aggregate_id BIGINT NOT NULL,
  payload JSON NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  retry_count INT NOT NULL DEFAULT 0,
  last_error VARCHAR(1000) NULL,
  next_retry_at DATETIME(3) NULL,
  sent_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_outbox_dispatch(status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_approval;
CREATE TABLE IF NOT EXISTS approval_flow (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resource_type_id BIGINT NOT NULL,
  version INT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_by BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_approval_flow(resource_type_id, version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS approval_node (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  flow_id BIGINT NOT NULL,
  level TINYINT NOT NULL,
  sequence_no INT NOT NULL DEFAULT 1,
  approver_role VARCHAR(30) NOT NULL,
  scope_type VARCHAR(30) NOT NULL DEFAULT 'RESOURCE',
  scope_value VARCHAR(100) NOT NULL DEFAULT '',
  approval_rule VARCHAR(20) NOT NULL DEFAULT 'ANY_ONE',
  quorum_count INT NULL,
  deadline_minutes INT NOT NULL DEFAULT 1440,
  UNIQUE KEY uk_approval_node(flow_id, level, sequence_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS approval_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  booking_id BIGINT NOT NULL,
  applicant_user_id BIGINT NULL,
  applicant_name VARCHAR(50) NULL,
  resource_id BIGINT NULL,
  resource_name VARCHAR(100) NULL,
  start_time DATETIME(3) NULL,
  end_time DATETIME(3) NULL,
  flow_version INT NOT NULL DEFAULT 1,
  level TINYINT NOT NULL,
  sequence_no INT NOT NULL DEFAULT 1,
  approver_role VARCHAR(30) NOT NULL,
  scope_type VARCHAR(30) NOT NULL DEFAULT 'RESOURCE',
  scope_value VARCHAR(100) NOT NULL DEFAULT '',
  approval_rule VARCHAR(20) NOT NULL DEFAULT 'ANY_ONE',
  assigned_user_id BIGINT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  deadline DATETIME(3) NOT NULL,
  completed_at DATETIME(3) NULL,
  comment VARCHAR(500) NULL,
  version INT NOT NULL DEFAULT 0,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_approval_task(booking_id, level, sequence_no),
  KEY idx_task_approver_status(assigned_user_id, status),
  KEY idx_task_role_scope_status(approver_role, scope_type, scope_value, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS approval_task_assignee (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'CANDIDATE',
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_task_assignee(task_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  task_id BIGINT NOT NULL,
  booking_id BIGINT NOT NULL,
  approver_id BIGINT NOT NULL,
  result VARCHAR(20) NOT NULL,
  comment VARCHAR(500) NULL,
  request_id VARCHAR(64) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_approval_record_request(request_id),
  KEY idx_approval_record_task(task_id, created_at)
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
CREATE TABLE IF NOT EXISTS notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  type VARCHAR(30) NOT NULL,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  is_read BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  read_at DATETIME(3) NULL,
  KEY idx_notification_user_read_time(user_id, is_read, created_at)
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
CREATE TABLE IF NOT EXISTS announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(200) NOT NULL,
  content TEXT NOT NULL,
  publisher_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  published_at DATETIME(3) NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_statistics;
CREATE TABLE IF NOT EXISTS statistics_snapshot (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  metric_type VARCHAR(40) NOT NULL,
  resource_id BIGINT NULL,
  user_id BIGINT NULL,
  period_start DATETIME(3) NOT NULL,
  period_end DATETIME(3) NOT NULL,
  numerator DECIMAL(18,2) NOT NULL,
  denominator DECIMAL(18,2) NULL,
  metric_value DECIMAL(18,6) NULL,
  calculated_until DATETIME(3) NOT NULL,
  data_version BIGINT NOT NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  UNIQUE KEY uk_statistics_metric(metric_type, resource_id, user_id, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS statistics_event_offset (
  consumer_name VARCHAR(100) PRIMARY KEY,
  last_event_id VARCHAR(64) NULL,
  last_created_at DATETIME(3) NULL,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_system;
CREATE TABLE IF NOT EXISTS system_config (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  config_key VARCHAR(100) NOT NULL UNIQUE,
  config_value VARCHAR(500) NOT NULL,
  value_type VARCHAR(20) NOT NULL DEFAULT 'STRING',
  description VARCHAR(500) NULL,
  updated_by BIGINT NOT NULL DEFAULT 0,
  updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  version INT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
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
CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  operator_id BIGINT NOT NULL,
  operation_type VARCHAR(50) NOT NULL,
  target_type VARCHAR(50) NULL,
  target_id BIGINT NULL,
  result VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
  reason VARCHAR(500) NULL,
  request_id VARCHAR(64) NOT NULL,
  ip VARCHAR(50) NULL,
  detail JSON NULL,
  created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  KEY idx_log_operator_time(operator_id, created_at),
  KEY idx_log_target_time(target_type, target_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

USE lab_user;
INSERT IGNORE INTO role(code, name, status) VALUES
  ('STUDENT', 'Student', 'ACTIVE'),
  ('TEACHER', 'Teacher', 'ACTIVE'),
  ('LAB_ADMIN', 'Lab administrator', 'ACTIVE'),
  ('SYSTEM_ADMIN', 'System administrator', 'ACTIVE');
