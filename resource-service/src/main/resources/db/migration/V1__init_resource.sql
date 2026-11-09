SET NAMES utf8mb4;

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

CREATE TABLE IF NOT EXISTS asset_purchase (
  id BIGINT NOT NULL AUTO_INCREMENT,
  purchaser_id BIGINT NULL,
  purchaser_name VARCHAR(255) NULL,
  purchased_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  source VARCHAR(30) NOT NULL DEFAULT 'PURCHASE',
  category_id BIGINT NULL,
  name VARCHAR(255) NOT NULL,
  brand VARCHAR(255) NULL,
  model VARCHAR(255) NULL,
  quantity INT NOT NULL,
  resource_id BIGINT NULL,
  location VARCHAR(255) NULL,
  asset_nos VARCHAR(2000) NULL,
  PRIMARY KEY (id),
  KEY idx_purchase_time (purchased_at)
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
