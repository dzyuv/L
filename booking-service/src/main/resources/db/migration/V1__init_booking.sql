SET NAMES utf8mb4;

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
