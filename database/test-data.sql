-- Laboratory booking platform demonstration data.
-- Prerequisite: run schema.sql first.
-- All demonstration account passwords are: 12345678
-- This file is repeatable: stable business keys are used to avoid duplicates.

SET NAMES utf8mb4;
SET @seed_now = NOW(3);
SET @next_monday = DATE_ADD(CURDATE(), INTERVAL (7 - WEEKDAY(CURDATE())) DAY);
SET @password_12345678 = '$2a$10$gbyx3ZiDdqJsoiOhphxuO..NN6rwrNLyg2JUmoDvXyiW3VEE8M/8q';

-- ---------------------------------------------------------------------------
-- Users, roles and permissions
-- ---------------------------------------------------------------------------
USE lab_user;

INSERT INTO role (code, name, status) VALUES
  ('STUDENT', '学生', 'ACTIVE'),
  ('TEACHER', '教师', 'ACTIVE'),
  ('LAB_ADMIN', '实验室管理员', 'ACTIVE'),
  ('SYSTEM_ADMIN', '系统管理员', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE';

INSERT INTO permission (code, name, status) VALUES
  ('resource:read', '查看资源', 'ACTIVE'),
  ('booking:create', '创建预约', 'ACTIVE'),
  ('booking:read:self', '查看个人预约', 'ACTIVE'),
  ('booking:cancel:self', '取消个人预约', 'ACTIVE'),
  ('booking:checkin', '预约签到', 'ACTIVE')
ON DUPLICATE KEY UPDATE name = VALUES(name), status = 'ACTIVE';

INSERT INTO `user`
  (employee_no, username, password_hash, real_name, email, phone, status,
   failed_login_count, token_version, created_at, updated_at, version, deleted)
VALUES
  ('S20260001', 'S20260001', @password_12345678, '张三', 'student@example.com', '13800000001', 'ACTIVE', 0, 0, @seed_now, @seed_now, 0, FALSE),
  ('S20260002', 'S20260002', @password_12345678, '李明', 'student2@example.com', '13800000002', 'ACTIVE', 0, 0, @seed_now, @seed_now, 0, FALSE),
  ('T20260001', 'T20260001', @password_12345678, '教师演示账号', 'teacher@example.com', '13900000001', 'ACTIVE', 0, 0, @seed_now, @seed_now, 0, FALSE),
  ('LAB20260001', 'LAB20260001', @password_12345678, '实验室管理员', 'lab-admin@example.com', '13900000002', 'ACTIVE', 0, 0, @seed_now, @seed_now, 0, FALSE),
  ('ADMIN20260001', 'ADMIN20260001', @password_12345678, '系统管理员', 'system-admin@example.com', '13900000003', 'ACTIVE', 0, 0, @seed_now, @seed_now, 0, FALSE)
ON DUPLICATE KEY UPDATE
  password_hash = VALUES(password_hash), real_name = VALUES(real_name),
  email = VALUES(email), phone = VALUES(phone), status = 'ACTIVE', deleted = FALSE;

SET @student_id = (SELECT id FROM `user` WHERE username = 'S20260001');
SET @student2_id = (SELECT id FROM `user` WHERE username = 'S20260002');
SET @teacher_id = (SELECT id FROM `user` WHERE username = 'T20260001');
SET @lab_admin_id = (SELECT id FROM `user` WHERE username = 'LAB20260001');
SET @system_admin_id = (SELECT id FROM `user` WHERE username = 'ADMIN20260001');
SET @student_role_id = (SELECT id FROM role WHERE code = 'STUDENT');
SET @teacher_role_id = (SELECT id FROM role WHERE code = 'TEACHER');
SET @lab_admin_role_id = (SELECT id FROM role WHERE code = 'LAB_ADMIN');
SET @system_admin_role_id = (SELECT id FROM role WHERE code = 'SYSTEM_ADMIN');

-- Demo accounts have fixed role boundaries. Clear stale assignments first so
-- rerunning this script also repairs role data left by earlier test versions.
DELETE FROM user_role
WHERE user_id IN (@student_id, @student2_id, @teacher_id, @lab_admin_id, @system_admin_id);

INSERT IGNORE INTO user_role (user_id, role_id) VALUES
  (@student_id, @student_role_id),
  (@student2_id, @student_role_id),
  (@teacher_id, @teacher_role_id),
  (@lab_admin_id, @lab_admin_role_id),
  (@lab_admin_id, @teacher_role_id),
  (@system_admin_id, @system_admin_role_id);

INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT @student_role_id, id FROM permission
WHERE code IN ('resource:read', 'booking:create', 'booking:read:self', 'booking:cancel:self', 'booking:checkin');
INSERT IGNORE INTO role_permission (role_id, permission_id)
SELECT @teacher_role_id, id FROM permission
WHERE code IN ('resource:read', 'booking:create', 'booking:read:self', 'booking:cancel:self', 'booking:checkin');

-- ---------------------------------------------------------------------------
-- Resource types, resources, weekly schedules and approval ownership
-- ---------------------------------------------------------------------------
USE lab_resource;

INSERT INTO resource_type
  (name, default_approval_level, default_need_checkin, enabled, version, deleted)
VALUES
  ('实验室', 1, TRUE, TRUE, 0, FALSE),
  ('机房', 1, TRUE, TRUE, 0, FALSE),
  ('会议室', 1, FALSE, TRUE, 0, FALSE),
  ('大型仪器', 1, TRUE, TRUE, 0, FALSE)
ON DUPLICATE KEY UPDATE
  default_approval_level = VALUES(default_approval_level),
  default_need_checkin = VALUES(default_need_checkin), enabled = TRUE, deleted = FALSE;

SET @laboratory_type_id = (SELECT id FROM resource_type WHERE name = '实验室');
SET @computer_room_type_id = (SELECT id FROM resource_type WHERE name = '机房');
SET @meeting_room_type_id = (SELECT id FROM resource_type WHERE name = '会议室');
SET @instrument_type_id = (SELECT id FROM resource_type WHERE name = '大型仪器');

INSERT INTO resource
  (type_id, name, location, capacity, status, description, owner_user_id,
   image_url, approval_level_override, need_checkin, max_duration_minutes, slot_minutes, version, deleted)
VALUES
  (@laboratory_type_id, '材料分析实验室', 'A201', 12, 'ACTIVE', '材料样品检测、显微观察与分析', @lab_admin_id,
   'https://images.unsplash.com/photo-1532094349884-543bc11b234d?auto=format&fit=crop&w=1200&q=80', 1, TRUE, 120, 30, 0, FALSE),
  (@computer_room_type_id, '计算机实验室', 'B305', 40, 'ACTIVE', '软件开发、人工智能课程与科研计算', @lab_admin_id,
   'https://images.unsplash.com/photo-1562774053-701939374585?auto=format&fit=crop&w=1200&q=80', 1, TRUE, 180, 30, 0, FALSE),
  (@laboratory_type_id, '电子测量实验室', 'C108', 20, 'ACTIVE', '电子电路、示波器和射频仪器测量', @lab_admin_id,
   'https://images.unsplash.com/photo-1581093458791-9d42e3c3d512?auto=format&fit=crop&w=1200&q=80', 1, TRUE, 120, 30, 0, FALSE),
  (@meeting_room_type_id, '科研讨论室', 'D206', 16, 'ACTIVE', '课题组讨论、项目评审与学术交流', @lab_admin_id,
   'https://images.unsplash.com/photo-1497366754035-f200968a6e72?auto=format&fit=crop&w=1200&q=80', NULL, FALSE, 180, 30, 0, FALSE),
  (@instrument_type_id, '频谱分析仪预约', 'C108-实验台02', 4, 'ACTIVE', '高价值频谱分析仪独立预约资源', @lab_admin_id,
   'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1200&q=80', 1, TRUE, 60, 30, 0, FALSE)
ON DUPLICATE KEY UPDATE
  location = VALUES(location), capacity = VALUES(capacity), status = 'ACTIVE',
  description = VALUES(description), owner_user_id = VALUES(owner_user_id),
  image_url = VALUES(image_url), approval_level_override = VALUES(approval_level_override),
  need_checkin = VALUES(need_checkin), max_duration_minutes = VALUES(max_duration_minutes),
  slot_minutes = VALUES(slot_minutes), deleted = FALSE;

SET @material_resource_id = (SELECT id FROM resource WHERE type_id = @laboratory_type_id AND name = '材料分析实验室');
SET @computer_resource_id = (SELECT id FROM resource WHERE type_id = @computer_room_type_id AND name = '计算机实验室');
SET @electronics_resource_id = (SELECT id FROM resource WHERE type_id = @laboratory_type_id AND name = '电子测量实验室');
SET @meeting_resource_id = (SELECT id FROM resource WHERE type_id = @meeting_room_type_id AND name = '科研讨论室');
SET @spectrum_resource_id = (SELECT id FROM resource WHERE type_id = @instrument_type_id AND name = '频谱分析仪预约');

INSERT INTO resource_schedule
  (resource_id, weekday, open_time, close_time, max_duration_minutes, slot_minutes, enabled, version)
SELECT seeded.resource_id, weekdays.weekday, '09:00:00', '17:00:00', seeded.max_minutes, 30, TRUE, 0
FROM (
  SELECT @material_resource_id resource_id, 120 max_minutes UNION ALL
  SELECT @computer_resource_id, 180 UNION ALL
  SELECT @electronics_resource_id, 120 UNION ALL
  SELECT @meeting_resource_id, 180 UNION ALL
  SELECT @spectrum_resource_id, 60
) seeded
CROSS JOIN (
  SELECT 1 weekday UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
) weekdays
WHERE NOT EXISTS (
  SELECT 1 FROM resource_schedule existing
  WHERE existing.resource_id = seeded.resource_id
    AND existing.weekday = weekdays.weekday
    AND existing.enabled = TRUE
);

DELETE FROM resource_manager WHERE manager_type = 'APPROVER';
INSERT INTO resource_manager
  (resource_id, user_id, manager_type, scope_type, scope_value, created_at)
VALUES
  (@material_resource_id, @teacher_id, 'OWNER', 'RESOURCE', '', @seed_now),
  (@electronics_resource_id, @teacher_id, 'OWNER', 'RESOURCE', '', @seed_now),
  (@spectrum_resource_id, @teacher_id, 'OWNER', 'RESOURCE', '', @seed_now),
  (@computer_resource_id, @lab_admin_id, 'OWNER', 'RESOURCE', '', @seed_now),
  (@meeting_resource_id, @lab_admin_id, 'OWNER', 'RESOURCE', '', @seed_now)
ON DUPLICATE KEY UPDATE manager_type = VALUES(manager_type), scope_type = VALUES(scope_type), scope_value = VALUES(scope_value);

-- A visible future closure used to verify calendar availability filtering.
INSERT INTO resource_closure
  (resource_id, start_time, end_time, reason, status, handled_booking_policy,
   created_by, created_at, updated_at, version)
SELECT @electronics_resource_id,
       TIMESTAMP(@next_monday, '14:00:00'), TIMESTAMP(@next_monday, '16:00:00'),
       '设备年度校准，暂停预约', 'PLANNED', 'KEEP_EXISTING',
       @lab_admin_id, @seed_now, @seed_now, 0
WHERE NOT EXISTS (
  SELECT 1 FROM resource_closure
  WHERE resource_id = @electronics_resource_id
    AND reason = '设备年度校准，暂停预约'
    AND status <> 'CANCELED'
    AND start_time >= CURDATE()
);

-- ---------------------------------------------------------------------------
-- Asset categories and one-record-per-device asset ledger
-- ---------------------------------------------------------------------------
INSERT INTO asset_category
  (name, serialized, high_value, enabled, description, created_at, updated_at, version)
VALUES
  ('计算机', TRUE, TRUE, TRUE, '台式机、笔记本和工作站，一台设备对应一条资产', @seed_now, @seed_now, 0),
  ('仪器设备', TRUE, TRUE, TRUE, '实验仪器和贵重设备，一台设备对应一条资产', @seed_now, @seed_now, 0),
  ('普通设备', TRUE, FALSE, TRUE, '需要独立编号和序列号的普通设备', @seed_now, @seed_now, 0)
ON DUPLICATE KEY UPDATE
  serialized = VALUES(serialized), high_value = VALUES(high_value),
  enabled = TRUE, description = VALUES(description);

SET @computer_category_id = (SELECT id FROM asset_category WHERE name = '计算机');
SET @instrument_category_id = (SELECT id FROM asset_category WHERE name = '仪器设备');
SET @normal_category_id = (SELECT id FROM asset_category WHERE name = '普通设备');

INSERT INTO asset
  (asset_no, name, category_id, resource_id, serial_no, brand, model, specification,
   status, location, custodian_user_id, purchase_date, warranty_until, original_cost,
   remark, deleted, created_at, updated_at, version)
VALUES
  ('LAB-PC-001', 'AI图形工作站', @computer_category_id, @computer_resource_id, 'SN-PC-2026-0001', 'Dell', 'Precision 5860', 'Xeon / 64GB / RTX 4090', 'IN_USE', 'B305-01', @teacher_id, '2026-01-12', '2029-01-11', 42800.00, '人工智能课程与科研计算', FALSE, @seed_now, @seed_now, 0),
  ('LAB-PC-002', '教学台式计算机', @computer_category_id, @computer_resource_id, 'SN-PC-2026-0002', 'Lenovo', 'ThinkCentre M90t', 'i7 / 32GB / 1TB SSD', 'REPORTED', 'B305-02', NULL, '2026-02-18', '2029-02-17', 8960.00, '计算机实验室备用机', FALSE, @seed_now, @seed_now, 0),
  ('LAB-PC-003', '移动工作站', @computer_category_id, @material_resource_id, 'SN-PC-2026-0003', 'HP', 'ZBook Fury 16', 'i9 / 64GB / RTX 3500 Ada', 'IN_USE', 'A201-资料柜', @teacher_id, '2025-11-03', '2028-11-02', 31900.00, '材料分析数据处理', FALSE, @seed_now, @seed_now, 0),
  ('LAB-PC-004', '实验数据服务器', @computer_category_id, @electronics_resource_id, 'SN-PC-2026-0004', 'Inspur', 'NF5180M6', '双路处理器 / 256GB / 24TB RAID', 'IN_USE', 'C108-机柜01', @teacher_id, '2025-09-20', '2028-09-19', 78500.00, '实验数据集中存储', FALSE, @seed_now, @seed_now, 0),
  ('LAB-INS-001', '数字示波器', @instrument_category_id, @electronics_resource_id, 'SN-INS-2026-0001', 'Tektronix', 'MSO54B', '500MHz / 4通道', 'REPORTED', 'C108-实验台01', @teacher_id, '2026-01-08', '2029-01-07', 126000.00, '电子测量核心仪器', FALSE, @seed_now, @seed_now, 0),
  ('LAB-INS-002', '频谱分析仪', @instrument_category_id, @spectrum_resource_id, 'SN-INS-2026-0002', 'Keysight', 'N9020B', '10Hz-26.5GHz', 'IN_USE', 'C108-实验台02', @teacher_id, '2025-12-16', '2028-12-15', 358000.00, '高价值精密仪器，一机一资产', FALSE, @seed_now, @seed_now, 0),
  ('LAB-INS-003', '精密电子天平', @instrument_category_id, @material_resource_id, 'SN-INS-2026-0003', 'Mettler Toledo', 'XPR205', '220g / 0.01mg', 'IN_STOCK', 'A201-称量区', NULL, '2026-03-05', '2028-03-04', 68500.00, '材料样品精密称量', FALSE, @seed_now, @seed_now, 0),
  ('LAB-INS-004', '荧光显微镜', @instrument_category_id, @material_resource_id, 'SN-INS-2026-0004', 'Olympus', 'BX53', 'LED荧光光源 / 研究级', 'MAINTENANCE', 'A201-暗室', @teacher_id, '2025-10-11', '2028-10-10', 218000.00, '材料表面形貌观察', FALSE, @seed_now, @seed_now, 0),
  ('LAB-INS-005', '红外热像仪', @instrument_category_id, @electronics_resource_id, 'SN-INS-2026-0005', 'FLIR', 'T865', '640x480 / -40至2000摄氏度', 'IN_STOCK', 'C108-仪器柜03', NULL, '2026-04-22', '2029-04-21', 145000.00, '电路温升与故障分析', FALSE, @seed_now, @seed_now, 0),
  ('LAB-EQP-001', '彩色激光打印机', @normal_category_id, @computer_resource_id, 'SN-EQP-2026-0001', 'HP', 'Color LaserJet Pro', 'A4彩色双面网络打印', 'IN_USE', 'B305-教师区', @teacher_id, '2025-08-15', '2027-08-14', 5600.00, '实验报告打印', FALSE, @seed_now, @seed_now, 0),
  ('LAB-EQP-002', '千兆网络交换机', @normal_category_id, @computer_resource_id, 'SN-EQP-2026-0002', 'H3C', 'S5130S-28P', '24口千兆可管理交换机', 'IN_USE', 'B305-机柜', @teacher_id, '2025-07-10', '2028-07-09', 7200.00, '实验室教学网络', FALSE, @seed_now, @seed_now, 0),
  ('LAB-EQP-003', '教学投影仪', @normal_category_id, @meeting_resource_id, 'SN-EQP-2026-0003', 'Epson', 'CB-L630U', '6200流明激光投影', 'IN_USE', 'D206-吊装位', NULL, '2024-05-06', '2027-05-05', 23800.00, '科研讨论室投影设备', FALSE, @seed_now, @seed_now, 0)
ON DUPLICATE KEY UPDATE
  name = VALUES(name), category_id = VALUES(category_id), resource_id = VALUES(resource_id),
  brand = VALUES(brand), model = VALUES(model), specification = VALUES(specification),
  status = VALUES(status), location = VALUES(location), custodian_user_id = VALUES(custodian_user_id),
  purchase_date = VALUES(purchase_date), warranty_until = VALUES(warranty_until),
  original_cost = VALUES(original_cost), remark = VALUES(remark), deleted = FALSE;

SET @pc_problem_asset_id = (SELECT id FROM asset WHERE asset_no = 'LAB-PC-002');
SET @scope_asset_id = (SELECT id FROM asset WHERE asset_no = 'LAB-INS-001');
SET @microscope_asset_id = (SELECT id FROM asset WHERE asset_no = 'LAB-INS-004');

INSERT INTO maintenance_ticket
  (ticket_no, asset_id, resource_id, location_snapshot, asset_clue, reported_by,
   previous_asset_status, report_type, severity, description, status, assigned_to,
   estimated_cost, actual_cost, resolution, processed_by, reported_at, processed_at,
   closed_at, created_at, updated_at, version)
VALUES
  ('TEST-MT-0001', @pc_problem_asset_id, @computer_resource_id, 'B305-02', '联想台式机，标签末尾0002', @student_id,
   'IN_USE', 'MALFUNCTION', 'MEDIUM', '开机后显示器无信号，重新插拔视频线后问题仍然存在。', 'REPORTED', NULL,
   NULL, NULL, NULL, NULL, DATE_SUB(@seed_now, INTERVAL 2 HOUR), NULL, NULL, DATE_SUB(@seed_now, INTERVAL 2 HOUR), @seed_now, 0),
  ('TEST-MT-0002', @scope_asset_id, @electronics_resource_id, 'C108-实验台01', '数字示波器一号通道', @teacher_id,
   'IN_USE', 'DAMAGE', 'HIGH', '示波器一号通道接口松动，探头接入后波形间歇性中断。', 'TRIAGED', '13800001111',
   1200.00, NULL, '工单已受理，等待进一步检查。', @lab_admin_id, DATE_SUB(@seed_now, INTERVAL 1 DAY), DATE_SUB(@seed_now, INTERVAL 20 HOUR), NULL, DATE_SUB(@seed_now, INTERVAL 1 DAY), @seed_now, 0),
  ('TEST-MT-0003', @microscope_asset_id, @material_resource_id, 'A201-暗室', '荧光显微镜载物台', @teacher_id,
   'IN_USE', 'MALFUNCTION', 'HIGH', '载物台移动阻力明显增大，需要停机检查传动组件。', 'REPAIRING', '13900002222',
   3500.00, NULL, '已停机并等待备件。', @lab_admin_id, DATE_SUB(@seed_now, INTERVAL 2 DAY), DATE_SUB(@seed_now, INTERVAL 1 DAY), NULL, DATE_SUB(@seed_now, INTERVAL 2 DAY), @seed_now, 0)
ON DUPLICATE KEY UPDATE
  asset_id = VALUES(asset_id), resource_id = VALUES(resource_id),
  location_snapshot = VALUES(location_snapshot), asset_clue = VALUES(asset_clue),
  severity = VALUES(severity), description = VALUES(description), status = VALUES(status),
  assigned_to = VALUES(assigned_to), estimated_cost = VALUES(estimated_cost),
  resolution = VALUES(resolution), processed_by = VALUES(processed_by), updated_at = @seed_now;

INSERT INTO asset_status_history
  (asset_id, from_status, to_status, reason, operator_id, created_at)
SELECT @pc_problem_asset_id, 'IN_USE', 'REPORTED', '测试报修工单 TEST-MT-0001', @student_id, DATE_SUB(@seed_now, INTERVAL 2 HOUR)
WHERE NOT EXISTS (SELECT 1 FROM asset_status_history WHERE asset_id = @pc_problem_asset_id AND reason = '测试报修工单 TEST-MT-0001');
INSERT INTO asset_status_history
  (asset_id, from_status, to_status, reason, operator_id, created_at)
SELECT @microscope_asset_id, 'IN_USE', 'MAINTENANCE', '测试报修工单 TEST-MT-0003', @lab_admin_id, DATE_SUB(@seed_now, INTERVAL 1 DAY)
WHERE NOT EXISTS (SELECT 1 FROM asset_status_history WHERE asset_id = @microscope_asset_id AND reason = '测试报修工单 TEST-MT-0003');

-- ---------------------------------------------------------------------------
-- Bookings. Dates are relative to the next Monday so the data stays useful.
-- ---------------------------------------------------------------------------
USE lab_booking;

INSERT INTO booking
  (booking_no, user_id, resource_id, applicant_name_snapshot, resource_name_snapshot,
   start_time, end_time, slot_minutes_snapshot, purpose, participants, status,
   approval_level_snapshot, approval_flow_version, approval_deadline,
   need_checkin_snapshot, completed_at, canceled_at, cancel_reason, forced,
   client_request_id, created_at, updated_at, version, deleted)
VALUES
  ('TEST-BK-0001', @student_id, @material_resource_id, '张三', '材料分析实验室',
   TIMESTAMP(@next_monday, '09:00:00'), TIMESTAMP(@next_monday, '10:00:00'), 30, '材料性能课程实验', 6, 'PENDING_APPROVAL',
   1, 1, TIMESTAMP(DATE_SUB(@next_monday, INTERVAL 1 DAY), '18:00:00'), TRUE, NULL, NULL, NULL, FALSE,
   'seed-booking-student-pending', @seed_now, @seed_now, 0, FALSE),
  ('TEST-BK-0002', @teacher_id, @electronics_resource_id, '教师演示账号', '电子测量实验室',
   TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '10:30:00'), 30, '科研项目电路测试', 4, 'PENDING_APPROVAL',
   1, 1, TIMESTAMP(@next_monday, '18:00:00'), TRUE, NULL, NULL, NULL, FALSE,
   'seed-booking-teacher-pending', @seed_now, @seed_now, 0, FALSE),
  ('TEST-BK-0003', @teacher_id, @meeting_resource_id, '教师演示账号', '科研讨论室',
   TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 2 DAY), '14:00:00'), TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 2 DAY), '15:30:00'), 30, '课题组周会', 10, 'APPROVED',
   0, NULL, NULL, FALSE, NULL, NULL, NULL, FALSE,
   'seed-booking-teacher-approved', @seed_now, @seed_now, 0, FALSE),
  ('TEST-BK-0004', @student2_id, @computer_resource_id, '李明', '计算机实验室',
   TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '11:00:00'), 30, '程序设计课程实验', 30, 'COMPLETED',
   1, 1, NULL, TRUE, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '11:00:00'), NULL, NULL, FALSE,
   'seed-booking-history-completed', DATE_SUB(@seed_now, INTERVAL 8 DAY), DATE_SUB(@seed_now, INTERVAL 7 DAY), 1, FALSE),
  ('TEST-BK-0005', @student_id, @meeting_resource_id, '张三', '科研讨论室',
   TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '15:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '16:00:00'), 30, '项目讨论', 5, 'CANCELED',
   0, NULL, NULL, FALSE, NULL, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '10:00:00'), '计划调整', FALSE,
   'seed-booking-history-canceled', DATE_SUB(@seed_now, INTERVAL 5 DAY), DATE_SUB(@seed_now, INTERVAL 4 DAY), 1, FALSE),
  ('TEST-BK-0006', @student_id, @computer_resource_id, '张三', '计算机实验室',
   TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 10 DAY), '09:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 10 DAY), '10:00:00'), 30, '上机练习', 2, 'NO_SHOW',
   1, 1, NULL, TRUE, NULL, NULL, NULL, FALSE,
   'seed-booking-history-noshow-zhang', DATE_SUB(@seed_now, INTERVAL 11 DAY), DATE_SUB(@seed_now, INTERVAL 10 DAY), 1, FALSE),
  ('TEST-BK-0007', @student2_id, @electronics_resource_id, '李明', '电子测量实验室',
   TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '14:00:00'), TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '15:00:00'), 30, '仪器操作训练', 3, 'NO_SHOW',
   1, 1, NULL, TRUE, NULL, NULL, NULL, FALSE,
   'seed-booking-history-noshow-li', DATE_SUB(@seed_now, INTERVAL 6 DAY), DATE_SUB(@seed_now, INTERVAL 5 DAY), 1, FALSE)
ON DUPLICATE KEY UPDATE
  user_id = VALUES(user_id), resource_id = VALUES(resource_id),
  applicant_name_snapshot = VALUES(applicant_name_snapshot), resource_name_snapshot = VALUES(resource_name_snapshot),
  start_time = VALUES(start_time), end_time = VALUES(end_time), purpose = VALUES(purpose),
  participants = VALUES(participants), status = VALUES(status), approval_deadline = VALUES(approval_deadline),
  completed_at = VALUES(completed_at), canceled_at = VALUES(canceled_at), cancel_reason = VALUES(cancel_reason),
  updated_at = @seed_now, deleted = FALSE;

SET @student_pending_booking_id = (SELECT id FROM booking WHERE booking_no = 'TEST-BK-0001');
SET @teacher_pending_booking_id = (SELECT id FROM booking WHERE booking_no = 'TEST-BK-0002');
SET @teacher_approved_booking_id = (SELECT id FROM booking WHERE booking_no = 'TEST-BK-0003');

INSERT INTO booking_slot (resource_id, booking_id, slot_start, released_at, release_reason, created_at)
SELECT desired.resource_id, desired.booking_id, desired.slot_start, NULL, NULL, @seed_now
FROM (
  SELECT @material_resource_id resource_id, @student_pending_booking_id booking_id, TIMESTAMP(@next_monday, '09:00:00') slot_start UNION ALL
  SELECT @material_resource_id, @student_pending_booking_id, TIMESTAMP(@next_monday, '09:30:00') UNION ALL
  SELECT @electronics_resource_id, @teacher_pending_booking_id, TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '09:00:00') UNION ALL
  SELECT @electronics_resource_id, @teacher_pending_booking_id, TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '09:30:00') UNION ALL
  SELECT @electronics_resource_id, @teacher_pending_booking_id, TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '10:00:00') UNION ALL
  SELECT @meeting_resource_id, @teacher_approved_booking_id, TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 2 DAY), '14:00:00') UNION ALL
  SELECT @meeting_resource_id, @teacher_approved_booking_id, TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 2 DAY), '14:30:00') UNION ALL
  SELECT @meeting_resource_id, @teacher_approved_booking_id, TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 2 DAY), '15:00:00')
) desired
WHERE NOT EXISTS (
  SELECT 1 FROM booking_slot existing
  WHERE existing.booking_id = desired.booking_id
    AND existing.slot_start = desired.slot_start
    AND existing.released_at IS NULL
);

INSERT INTO booking_status_history
  (booking_id, from_status, to_status, operator_id, reason, request_id, created_at)
SELECT id, NULL, status, user_id, '测试数据初始化', CONCAT('seed-history-', booking_no), created_at
FROM booking seeded
WHERE booking_no LIKE 'TEST-BK-%'
  AND NOT EXISTS (
    SELECT 1 FROM booking_status_history history
    WHERE history.booking_id = seeded.id
      AND BINARY history.request_id = BINARY CONCAT('seed-history-', seeded.booking_no)
  );

INSERT INTO violation_record (booking_id, user_id, violation_type, status, comment, created_at)
SELECT id, user_id, 'NO_SHOW', 'OPEN', '预约时段未签到', DATE_SUB(@seed_now, INTERVAL 10 DAY)
FROM booking WHERE booking_no = 'TEST-BK-0006'
  AND NOT EXISTS (SELECT 1 FROM violation_record existing WHERE existing.booking_id = booking.id AND existing.violation_type = 'NO_SHOW');
INSERT INTO violation_record (booking_id, user_id, violation_type, status, comment, processed_at, created_at)
SELECT id, user_id, 'NO_SHOW', 'CONFIRMED', '预约时段未签到，已确认违约', DATE_SUB(@seed_now, INTERVAL 4 DAY), DATE_SUB(@seed_now, INTERVAL 5 DAY)
FROM booking WHERE booking_no = 'TEST-BK-0007'
  AND NOT EXISTS (SELECT 1 FROM violation_record existing WHERE existing.booking_id = booking.id AND existing.violation_type = 'NO_SHOW');

-- ---------------------------------------------------------------------------
-- Approval tasks. The teacher sees the student's task; the laboratory
-- administrator receives the teacher's own request, preventing self-approval.
-- ---------------------------------------------------------------------------
USE lab_approval;

INSERT INTO approval_task
  (booking_id, applicant_user_id, applicant_name, resource_id, resource_name,
   start_time, end_time, flow_version, level, sequence_no, approver_role,
   scope_type, scope_value, approval_rule, assigned_user_id, status,
   deadline, version, created_at)
SELECT @student_pending_booking_id, @student_id, '张三', @material_resource_id, '材料分析实验室',
       TIMESTAMP(@next_monday, '09:00:00'), TIMESTAMP(@next_monday, '10:00:00'), 1, 1, 1, 'TEACHER',
       'RESOURCE', CAST(@material_resource_id AS CHAR), 'ANY_ONE', @teacher_id, 'PENDING',
       TIMESTAMP(DATE_SUB(@next_monday, INTERVAL 1 DAY), '18:00:00'), 0, @seed_now
WHERE NOT EXISTS (
  SELECT 1 FROM approval_task
  WHERE booking_id = @student_pending_booking_id AND level = 1 AND sequence_no = 1
);

INSERT INTO approval_task
  (booking_id, applicant_user_id, applicant_name, resource_id, resource_name,
   start_time, end_time, flow_version, level, sequence_no, approver_role,
   scope_type, scope_value, approval_rule, assigned_user_id, status,
   deadline, version, created_at)
SELECT @teacher_pending_booking_id, @teacher_id, '教师演示账号', @electronics_resource_id, '电子测量实验室',
       TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '09:00:00'), TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '10:30:00'), 1, 1, 1, 'LAB_ADMIN',
       'RESOURCE', CAST(@electronics_resource_id AS CHAR), 'ANY_ONE', @lab_admin_id, 'PENDING',
       TIMESTAMP(@next_monday, '18:00:00'), 0, @seed_now
WHERE NOT EXISTS (
  SELECT 1 FROM approval_task
  WHERE booking_id = @teacher_pending_booking_id AND level = 1 AND sequence_no = 1
);

UPDATE approval_task SET
  applicant_user_id = @student_id, applicant_name = '张三',
  resource_id = @material_resource_id, resource_name = '材料分析实验室',
  start_time = TIMESTAMP(@next_monday, '09:00:00'), end_time = TIMESTAMP(@next_monday, '10:00:00'),
  approver_role = 'TEACHER', scope_value = CAST(@material_resource_id AS CHAR),
  assigned_user_id = @teacher_id, status = 'PENDING',
  deadline = TIMESTAMP(DATE_SUB(@next_monday, INTERVAL 1 DAY), '18:00:00')
WHERE booking_id = @student_pending_booking_id AND level = 1 AND sequence_no = 1;

UPDATE approval_task SET
  applicant_user_id = @teacher_id, applicant_name = '教师演示账号',
  resource_id = @electronics_resource_id, resource_name = '电子测量实验室',
  start_time = TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '09:00:00'),
  end_time = TIMESTAMP(DATE_ADD(@next_monday, INTERVAL 1 DAY), '10:30:00'),
  approver_role = 'LAB_ADMIN', scope_value = CAST(@electronics_resource_id AS CHAR),
  assigned_user_id = @lab_admin_id, status = 'PENDING', deadline = TIMESTAMP(@next_monday, '18:00:00')
WHERE booking_id = @teacher_pending_booking_id AND level = 1 AND sequence_no = 1;

-- ---------------------------------------------------------------------------
-- Common system configuration
-- ---------------------------------------------------------------------------
USE lab_system;

INSERT INTO system_config
  (config_key, config_value, value_type, description, updated_by, updated_at, version)
VALUES
  ('checkin.window.before_minutes', '15', 'INT', '签到提前时间（分钟）', @system_admin_id, @seed_now, 0),
  ('checkin.window.after_minutes', '30', 'INT', '签到延后时间（分钟）', @system_admin_id, @seed_now, 0),
  ('booking.default_max_duration', '120', 'INT', '默认最大预约时长', @system_admin_id, @seed_now, 0),
  ('booking.slot_minutes', '30', 'INT', '默认预约粒度', @system_admin_id, @seed_now, 0),
  ('violation.max_count', '3', 'INT', '最大违约次数', @system_admin_id, @seed_now, 0),
  ('approval.timeout_minutes', '1440', 'INT', '审批超时时间', @system_admin_id, @seed_now, 0)
ON DUPLICATE KEY UPDATE
  config_value = VALUES(config_value), value_type = VALUES(value_type),
  description = VALUES(description), updated_by = VALUES(updated_by), updated_at = @seed_now;

SELECT 'Test data initialized successfully' AS result,
       @next_monday AS generated_booking_week,
       'S20260001 / T20260001 / LAB20260001 / ADMIN20260001' AS accounts,
       '12345678' AS password;
