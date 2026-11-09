-- DESTRUCTIVE maintenance script. Do NOT run this against any database that
-- contains data you want to keep. It removes legacy databases and tables that
-- the current schema no longer manages.
--
-- Usage (only on a disposable local environment):
--   mysql -h127.0.0.1 -P3306 -uroot -p < database/manual/reset.sql

DROP DATABASE IF EXISTS lab_notification;
DROP DATABASE IF EXISTS lab_statistics;
DROP TABLE IF EXISTS lab_user.users;
DROP TABLE IF EXISTS lab_resource.resources;
DROP TABLE IF EXISTS lab_booking.booking_participant;
DROP TABLE IF EXISTS lab_booking.idempotency_record;
DROP TABLE IF EXISTS lab_booking.outbox_event;
DROP TABLE IF EXISTS lab_approval.approval_task_assignee;
DROP TABLE IF EXISTS lab_approval.approval_node;
DROP TABLE IF EXISTS lab_approval.approval_flow;
DROP TABLE IF EXISTS lab_system.data_dictionary;
