SET NAMES utf8mb4;

ALTER TABLE approval_task
  MODIFY comment VARCHAR(500) NULL;

ALTER TABLE approval_record
  MODIFY comment VARCHAR(500) NULL;

ALTER TABLE booking_status_history
  MODIFY reason VARCHAR(500) NULL;
