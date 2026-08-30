SET @exists := (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema='lab_booking' AND table_name='booking' AND column_name='reject_reason');
SET @ddl := IF(@exists=0, 'ALTER TABLE lab_booking.booking ADD COLUMN reject_reason VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
