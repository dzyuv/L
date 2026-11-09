SET NAMES utf8mb4;

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
