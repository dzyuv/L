SET NAMES utf8mb4;

ALTER TABLE resource
  MODIFY description VARCHAR(500) NULL,
  MODIFY image_url VARCHAR(500) NULL;

ALTER TABLE resource_closure
  MODIFY reason VARCHAR(500) NULL;

ALTER TABLE asset_category
  MODIFY description VARCHAR(500) NULL;

ALTER TABLE asset
  MODIFY remark VARCHAR(1000) NULL,
  MODIFY specification VARCHAR(500) NULL;

ALTER TABLE asset_status_history
  MODIFY reason VARCHAR(500) NULL;

ALTER TABLE maintenance_ticket
  MODIFY asset_clue VARCHAR(500) NULL,
  MODIFY resolution VARCHAR(2000) NULL;
