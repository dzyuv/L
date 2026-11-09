SET NAMES utf8mb4;

DELETE duplicate_row FROM approval_record duplicate_row
JOIN approval_record kept_row
  ON duplicate_row.request_id = kept_row.request_id
 AND duplicate_row.task_id = kept_row.task_id
 AND duplicate_row.id > kept_row.id
WHERE duplicate_row.request_id IS NOT NULL;

ALTER TABLE approval_record
  DROP KEY idx_approval_record_request,
  ADD UNIQUE KEY uk_approval_record_request (request_id, task_id);
