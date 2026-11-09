SET NAMES utf8mb4;

ALTER TABLE booking
  MODIFY purpose VARCHAR(500) NULL;

ALTER TABLE violation_record
  MODIFY comment VARCHAR(500) NULL;
