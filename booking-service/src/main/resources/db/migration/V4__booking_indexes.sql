ALTER TABLE approval_task
  ADD KEY idx_approval_pending_assignee (status, assigned_user_id),
  ADD KEY idx_approval_pending_role (status, approver_role, assigned_user_id),
  ADD KEY idx_approval_booking_level (booking_id, level),
  ADD KEY idx_approval_booking_status (booking_id, status);

ALTER TABLE approval_record
  ADD KEY idx_approval_record_task (task_id),
  ADD KEY idx_approval_record_booking (booking_id),
  ADD KEY idx_approval_record_request (request_id, task_id);

ALTER TABLE booking
  ADD KEY idx_booking_resource_status_time (resource_id, status, start_time),
  ADD KEY idx_booking_status_time (status, start_time),
  ADD KEY idx_booking_user_status_time (user_id, status, start_time),
  ADD KEY idx_booking_user_start (user_id, start_time),
  ADD KEY idx_booking_start_time (start_time);

ALTER TABLE booking_slot
  ADD KEY idx_slot_booking_released (booking_id, released_at);

ALTER TABLE violation_record
  ADD KEY idx_violation_user_type_time (user_id, violation_type, status, created_at);
