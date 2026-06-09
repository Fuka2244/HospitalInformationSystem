-- =====================================================
-- Staff portal demo data for HIS
-- Covers doctor, pharmacist, and admin workstations.
--
-- Prerequisites:
-- 1. Run src/main/resources/sql/his_schema.sql
-- 2. Run src/main/resources/sql/multi_role_extension.sql
-- 3. Run src/main/resources/sql/staff_auth_extension.sql
--
-- This script only touches demo IDs in the 9000+ / 1000+ range and can be
-- executed repeatedly.
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- Clean previously generated demo rows.
DELETE FROM medicine_stock_log WHERE id BETWEEN 9001 AND 9010;
DELETE FROM prescription_audit WHERE id BETWEEN 9001 AND 9010;
DELETE FROM prescription_item WHERE id BETWEEN 9001 AND 9010;
DELETE FROM prescription WHERE id BETWEEN 9001 AND 9010;
DELETE FROM medical_report WHERE id BETWEEN 9001 AND 9010;
DELETE FROM billing WHERE id BETWEEN 9001 AND 9010;
DELETE FROM visit_record WHERE id BETWEEN 9001 AND 9010;
DELETE FROM appointment WHERE id BETWEEN 9001 AND 9010;
DELETE FROM medical_record WHERE id BETWEEN 9001 AND 9010;
DELETE FROM doctor_schedule WHERE id BETWEEN 9001 AND 9010;

DELETE FROM medicine_inventory WHERE medicine_id BETWEEN 1001 AND 1010;
DELETE FROM medicine WHERE id BETWEEN 1001 AND 1010;
DELETE FROM pharmacist WHERE id BETWEEN 1001 AND 1010;
DELETE FROM doctor WHERE id BETWEEN 1001 AND 1010;
DELETE FROM department WHERE id BETWEEN 101 AND 110;
DELETE FROM admin WHERE id BETWEEN 1001 AND 1010;
DELETE FROM patient WHERE account IN (
  'demo-patient-0001-0001-000000000001',
  'demo-patient-0002-0002-000000000002',
  'demo-patient-0003-0003-000000000003',
  'demo-patient-0004-0004-000000000004',
  'demo-patient-0005-0005-000000000005'
);

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================
-- Base organization data
-- =====================================================

INSERT INTO department (id, name, description, location, status, create_time, update_time) VALUES
(101, '心内科', '高血压、冠心病、心律失常、心力衰竭等心血管疾病诊疗。', '门诊楼 2F A区', 1, NOW(), NOW()),
(102, '呼吸内科', '慢性咳嗽、哮喘、肺炎、慢阻肺等呼吸系统疾病诊疗。', '门诊楼 2F B区', 1, NOW(), NOW()),
(103, '骨科', '关节疼痛、骨折、运动损伤、脊柱疾病诊疗。', '门诊楼 3F A区', 1, NOW(), NOW()),
(104, '儿科', '儿童发热、咳嗽、腹泻及生长发育咨询。', '门诊楼 1F C区', 1, NOW(), NOW()),
(105, '药剂科', '处方审核、药品发放、库存与用药咨询。', '医技楼 1F 药房', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  description = VALUES(description),
  location = VALUES(location),
  status = VALUES(status),
  update_time = NOW();

-- Demo password for all staff accounts: 123456
SET @demo_staff_password = '$2a$10$09BFwxsJX3my0XsPDpIi/uC.n8Tyr2Wb/oylH0gSGOaMSVjRw.yaK';

INSERT INTO doctor (id, username, password, name, gender, age, title, department_id, specialty, phone, status, create_time, update_time) VALUES
(1001, 'doctor_lin', @demo_staff_password, '林远', '男', 46, '主任医师', 101, '高血压、冠心病、胸痛评估', '13810001001', 1, NOW(), NOW()),
(1002, 'doctor_shen', @demo_staff_password, '沈清', '女', 39, '副主任医师', 102, '哮喘、慢性咳嗽、肺部感染', '13810001002', 1, NOW(), NOW()),
(1003, 'doctor_zhou', @demo_staff_password, '周景', '男', 42, '副主任医师', 103, '膝关节损伤、骨关节炎、运动康复', '13810001003', 1, NOW(), NOW()),
(1004, 'doctor_xu', @demo_staff_password, '许安', '女', 35, '主治医师', 104, '儿童发热、呼吸道感染、消化不良', '13810001004', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  name = VALUES(name),
  gender = VALUES(gender),
  age = VALUES(age),
  title = VALUES(title),
  department_id = VALUES(department_id),
  specialty = VALUES(specialty),
  phone = VALUES(phone),
  status = VALUES(status),
  update_time = NOW();

INSERT INTO pharmacist (id, username, password, name, gender, phone, status, create_time, update_time) VALUES
(1001, 'pharm_zhao', @demo_staff_password, '赵宁', '女', '13820001001', 1, NOW(), NOW()),
(1002, 'pharm_qian', @demo_staff_password, '钱嘉', '男', '13820001002', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  name = VALUES(name),
  gender = VALUES(gender),
  phone = VALUES(phone),
  status = VALUES(status),
  update_time = NOW();

-- Password comments only. Current staff portals are guarded by session role/account;
-- there is no dedicated doctor/pharmacist/admin login controller yet.
INSERT INTO admin (id, username, password, name, phone, role, create_time, update_time) VALUES
(1001, 'demo_admin', @demo_staff_password, '演示管理员', '13830001001', 'admin', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  password = VALUES(password),
  name = VALUES(name),
  phone = VALUES(phone),
  role = VALUES(role),
  update_time = NOW();

INSERT INTO patient (account, username, name, password, gender, age, id_card, phone, address, avatar) VALUES
('demo-patient-0001-0001-000000000001', 'demo_chen', '陈晨', '$2a$10$Q0nI2vjY7w3dGZKyLMFziO.BACVZv6Kq6cYGzn.gfej18sPX.5bR2', '女', 32, '110101199402011221', '13910001001', '北京市朝阳区建国路 18 号', NULL),
('demo-patient-0002-0002-000000000002', 'demo_li', '李航', '$2a$10$Q0nI2vjY7w3dGZKyLMFziO.BACVZv6Kq6cYGzn.gfej18sPX.5bR2', '男', 51, '110101197501021222', '13910001002', '北京市海淀区学院路 66 号', NULL),
('demo-patient-0003-0003-000000000003', 'demo_wang', '王可', '$2a$10$Q0nI2vjY7w3dGZKyLMFziO.BACVZv6Kq6cYGzn.gfej18sPX.5bR2', '女', 7, '110101201903031223', '13910001003', '北京市西城区复兴门 9 号', NULL),
('demo-patient-0004-0004-000000000004', 'demo_zhou', '周明', '$2a$10$Q0nI2vjY7w3dGZKyLMFziO.BACVZv6Kq6cYGzn.gfej18sPX.5bR2', '男', 28, '110101199804041224', '13910001004', '北京市丰台区南苑路 120 号', NULL),
('demo-patient-0005-0005-000000000005', 'demo_yu', '余静', '$2a$10$Q0nI2vjY7w3dGZKyLMFziO.BACVZv6Kq6cYGzn.gfej18sPX.5bR2', '女', 44, '110101198205051225', '13910001005', '北京市东城区东四十条 28 号', NULL)
ON DUPLICATE KEY UPDATE
  username = VALUES(username),
  name = VALUES(name),
  password = VALUES(password),
  gender = VALUES(gender),
  age = VALUES(age),
  id_card = VALUES(id_card),
  phone = VALUES(phone),
  address = VALUES(address),
  avatar = VALUES(avatar);

-- =====================================================
-- Doctor workstation data
-- Today queue: status 0 waiting, 1 called, 2 visiting.
-- Historical rows support medical-record dialog and admin statistics.
-- =====================================================

INSERT INTO doctor_schedule (id, doctor_id, schedule_date, time_slot, max_patients, booked_count, status) VALUES
(9001, 1001, CURDATE(), '08:00-09:00', 12, 4, 1),
(9002, 1001, CURDATE(), '09:00-10:00', 12, 6, 1),
(9003, 1001, CURDATE(), '10:00-11:00', 12, 5, 1),
(9004, 1002, CURDATE(), '08:00-09:00', 10, 3, 1),
(9005, 1003, DATE_ADD(CURDATE(), INTERVAL 1 DAY), '14:00-15:00', 10, 2, 1),
(9006, 1004, DATE_ADD(CURDATE(), INTERVAL 2 DAY), '09:00-10:00', 14, 4, 1)
ON DUPLICATE KEY UPDATE
  doctor_id = VALUES(doctor_id),
  schedule_date = VALUES(schedule_date),
  time_slot = VALUES(time_slot),
  max_patients = VALUES(max_patients),
  booked_count = VALUES(booked_count),
  status = VALUES(status);

INSERT INTO appointment (
  id, patient_id, doctor_id, department_id, appointment_type, examination_type,
  appointment_date, time_slot, status, cancel_reason, ai_recommended, location,
  registration_status, create_time, update_time
) VALUES
(9001, 'demo-patient-0001-0001-000000000001', 1001, 101, 'DOCTOR', NULL, CURDATE(), '08:00-09:00', 0, NULL, 1, NULL, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(9002, 'demo-patient-0002-0002-000000000002', 1001, 101, 'DOCTOR', NULL, CURDATE(), '09:00-10:00', 1, NULL, 0, '门诊楼 2F A201', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(9003, 'demo-patient-0005-0005-000000000005', 1001, 101, 'DOCTOR', NULL, CURDATE(), '10:00-11:00', 2, NULL, 0, '门诊楼 2F A201', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(9004, 'demo-patient-0003-0003-000000000003', 1004, 104, 'DOCTOR', NULL, CURDATE(), '08:00-09:00', 0, NULL, 1, NULL, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(9005, 'demo-patient-0004-0004-000000000004', 1003, 103, 'DOCTOR', NULL, DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:00-15:00', 3, NULL, 0, '门诊楼 3F A302', 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(9006, 'demo-patient-0001-0001-000000000001', 1002, 102, 'DOCTOR', NULL, DATE_SUB(CURDATE(), INTERVAL 2 DAY), '09:00-10:00', -1, '患者临时有事', 0, NULL, 0, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY))
ON DUPLICATE KEY UPDATE
  patient_id = VALUES(patient_id),
  doctor_id = VALUES(doctor_id),
  department_id = VALUES(department_id),
  appointment_type = VALUES(appointment_type),
  examination_type = VALUES(examination_type),
  appointment_date = VALUES(appointment_date),
  time_slot = VALUES(time_slot),
  status = VALUES(status),
  cancel_reason = VALUES(cancel_reason),
  ai_recommended = VALUES(ai_recommended),
  location = VALUES(location),
  registration_status = VALUES(registration_status),
  update_time = VALUES(update_time);

INSERT INTO visit_record (
  id, appointment_id, patient_id, doctor_id, department_id, visit_number,
  call_status, call_time, check_in_time, visit_start_time, visit_end_time,
  diagnosis, treatment, notes, create_time, update_time
) VALUES
(9001, 9002, 'demo-patient-0002-0002-000000000002', 1001, 101, 'A0902', 1, NOW(), NULL, NULL, NULL, NULL, NULL, '已叫号，等待进入诊室。', NOW(), NOW()),
(9002, 9003, 'demo-patient-0005-0005-000000000005', 1001, 101, 'A1003', 1, DATE_SUB(NOW(), INTERVAL 25 MINUTE), DATE_SUB(NOW(), INTERVAL 20 MINUTE), DATE_SUB(NOW(), INTERVAL 18 MINUTE), NULL, NULL, NULL, '正在问诊。', NOW(), NOW()),
(9003, 9005, 'demo-patient-0004-0004-000000000004', 1003, 103, 'B1405', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY), '右膝关节扭伤', '休息制动，冷敷，必要时口服止痛药。', '建议一周后复查。', DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE
  appointment_id = VALUES(appointment_id),
  patient_id = VALUES(patient_id),
  doctor_id = VALUES(doctor_id),
  department_id = VALUES(department_id),
  visit_number = VALUES(visit_number),
  call_status = VALUES(call_status),
  call_time = VALUES(call_time),
  check_in_time = VALUES(check_in_time),
  visit_start_time = VALUES(visit_start_time),
  visit_end_time = VALUES(visit_end_time),
  diagnosis = VALUES(diagnosis),
  treatment = VALUES(treatment),
  notes = VALUES(notes),
  update_time = VALUES(update_time);

INSERT INTO medical_record (
  id, patient_id, doctor_id, department_id, chief_complaint, present_illness,
  diagnosis, treatment_plan, visit_date, status, create_time, update_time
) VALUES
(9001, 'demo-patient-0001-0001-000000000001', 1002, 102, '咳嗽 5 天，夜间加重。', '患者 5 天前受凉后出现咳嗽，少量白痰，无明显胸痛和呼吸困难。', '急性支气管炎', '多饮水，避免刺激性气味，口服祛痰药，必要时复诊。', DATE_SUB(NOW(), INTERVAL 2 DAY), 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9002, 'demo-patient-0002-0002-000000000002', 1001, 101, '血压升高 3 月。', '近 3 月多次测量血压偏高，最高 158/96mmHg，偶有头晕。', '原发性高血压 1 级', '低盐饮食，规律运动，监测血压，必要时启动降压药。', DATE_SUB(NOW(), INTERVAL 14 DAY), 1, DATE_SUB(NOW(), INTERVAL 14 DAY), DATE_SUB(NOW(), INTERVAL 14 DAY)),
(9003, 'demo-patient-0004-0004-000000000004', 1003, 103, '右膝疼痛 1 天。', '运动后出现右膝疼痛，活动受限，无明显畸形。', '右膝关节扭伤', '休息制动，冷敷，口服布洛芬缓释胶囊。', DATE_SUB(NOW(), INTERVAL 3 DAY), 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
(9004, 'demo-patient-0005-0005-000000000005', 1001, 101, '胸闷 1 周。', '活动后胸闷，休息后缓解，无晕厥。', '胸闷待查', '完善心电图和心肌酶检查，注意休息。', DATE_SUB(NOW(), INTERVAL 20 DAY), 1, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_SUB(NOW(), INTERVAL 20 DAY))
ON DUPLICATE KEY UPDATE
  patient_id = VALUES(patient_id),
  doctor_id = VALUES(doctor_id),
  department_id = VALUES(department_id),
  chief_complaint = VALUES(chief_complaint),
  present_illness = VALUES(present_illness),
  diagnosis = VALUES(diagnosis),
  treatment_plan = VALUES(treatment_plan),
  visit_date = VALUES(visit_date),
  status = VALUES(status),
  update_time = VALUES(update_time);

-- =====================================================
-- Pharmacist workstation data
-- =====================================================

INSERT INTO medicine (
  id, name, generic_name, category, specification, manufacturer, ingredients,
  efficacy, side_effects, contraindications, price, stock, status, create_time, update_time
) VALUES
(1001, '布洛芬缓释胶囊', '布洛芬', '化学药品', '0.3g*20粒', '示例制药有限公司', '布洛芬', '缓解轻至中度疼痛和发热。', '偶见胃部不适。', '活动性消化道溃疡患者禁用。', 18.50, 180, 1, NOW(), NOW()),
(1002, '盐酸氨溴索片', '氨溴索', '化学药品', '30mg*20片', '示例制药有限公司', '盐酸氨溴索', '用于痰液黏稠不易咳出。', '偶见恶心、胃部不适。', '对本品过敏者禁用。', 16.00, 60, 1, NOW(), NOW()),
(1003, '硝苯地平缓释片', '硝苯地平', '化学药品', '10mg*30片', '示例制药有限公司', '硝苯地平', '用于高血压、心绞痛。', '可见头痛、面部潮红。', '低血压患者慎用。', 22.00, 24, 1, NOW(), NOW()),
(1004, '小儿氨酚黄那敏颗粒', '复方制剂', '化学药品', '6g*10袋', '示例制药有限公司', '对乙酰氨基酚等', '用于儿童普通感冒引起的发热、鼻塞。', '可见嗜睡、口干。', '严重肝肾功能不全者禁用。', 13.80, 8, 1, NOW(), NOW()),
(1005, '阿莫西林胶囊', '阿莫西林', '抗菌药物', '0.5g*24粒', '示例制药有限公司', '阿莫西林', '用于敏感菌感染。', '可见皮疹、胃肠道反应。', '青霉素过敏者禁用。', 19.00, 200, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  name = VALUES(name),
  generic_name = VALUES(generic_name),
  category = VALUES(category),
  specification = VALUES(specification),
  manufacturer = VALUES(manufacturer),
  ingredients = VALUES(ingredients),
  efficacy = VALUES(efficacy),
  side_effects = VALUES(side_effects),
  contraindications = VALUES(contraindications),
  price = VALUES(price),
  stock = VALUES(stock),
  status = VALUES(status),
  update_time = NOW();

INSERT INTO prescription (id, medical_record_id, patient_id, doctor_id, prescription_date, status, create_time, update_time) VALUES
(9001, 9001, 'demo-patient-0001-0001-000000000001', 1002, DATE_SUB(NOW(), INTERVAL 2 DAY), 0, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(9002, 9002, 'demo-patient-0002-0002-000000000002', 1001, DATE_SUB(NOW(), INTERVAL 14 DAY), 0, DATE_SUB(NOW(), INTERVAL 14 DAY), NOW()),
(9003, 9003, 'demo-patient-0004-0004-000000000004', 1003, DATE_SUB(NOW(), INTERVAL 3 DAY), 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(9004, 9004, 'demo-patient-0005-0005-000000000005', 1001, DATE_SUB(NOW(), INTERVAL 20 DAY), 2, DATE_SUB(NOW(), INTERVAL 20 DAY), NOW())
ON DUPLICATE KEY UPDATE
  medical_record_id = VALUES(medical_record_id),
  patient_id = VALUES(patient_id),
  doctor_id = VALUES(doctor_id),
  prescription_date = VALUES(prescription_date),
  status = VALUES(status),
  update_time = NOW();

INSERT INTO prescription_item (id, prescription_id, medicine_id, dosage, quantity, days, remark) VALUES
(9001, 9001, 1002, '口服，每次 30mg，每日 3 次。', 20, 5, '饭后服用，多饮水。'),
(9002, 9002, 1003, '口服，每次 10mg，每日 2 次。', 30, 15, '监测血压，避免突然停药。'),
(9003, 9003, 1001, '口服，每次 0.3g，每日 2 次。', 20, 5, '饭后服用。'),
(9004, 9004, 1005, '口服，每次 0.5g，每日 3 次。', 24, 7, '青霉素过敏者禁用。')
ON DUPLICATE KEY UPDATE
  prescription_id = VALUES(prescription_id),
  medicine_id = VALUES(medicine_id),
  dosage = VALUES(dosage),
  quantity = VALUES(quantity),
  days = VALUES(days),
  remark = VALUES(remark);

INSERT INTO prescription_audit (
  id, prescription_id, pharmacist_id, audit_status, audit_remark,
  audit_time, create_time, update_time
) VALUES
(9001, 9003, 1001, 1, '用药剂量合理，审核通过。', DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9002, 9004, 1002, 2, '需补充过敏史后重新开具。', DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 10 DAY))
ON DUPLICATE KEY UPDATE
  prescription_id = VALUES(prescription_id),
  pharmacist_id = VALUES(pharmacist_id),
  audit_status = VALUES(audit_status),
  audit_remark = VALUES(audit_remark),
  audit_time = VALUES(audit_time),
  update_time = VALUES(update_time);

INSERT INTO medicine_inventory (
  id, medicine_id, quantity, min_stock, max_stock, purchase_price, selling_price,
  supplier, batch_number, expiry_date, create_time, update_time
) VALUES
(9001, 1001, 180, 30, 500, 9.80, 18.50, '华北医药配送中心', 'IBU202606', DATE_ADD(NOW(), INTERVAL 18 MONTH), NOW(), NOW()),
(9002, 1002, 60, 25, 400, 8.20, 16.00, '华北医药配送中心', 'AMB202605', DATE_ADD(NOW(), INTERVAL 12 MONTH), NOW(), NOW()),
(9003, 1003, 24, 30, 300, 11.00, 22.00, '京西医药供应链', 'NIF202604', DATE_ADD(NOW(), INTERVAL 10 MONTH), NOW(), NOW()),
(9004, 1004, 8, 20, 240, 6.50, 13.80, '儿童用药配送中心', 'PED202603', DATE_ADD(NOW(), INTERVAL 9 MONTH), NOW(), NOW()),
(9005, 1005, 200, 40, 600, 10.20, 19.00, '华北医药配送中心', 'AMX202607', DATE_ADD(NOW(), INTERVAL 20 MONTH), NOW(), NOW())
ON DUPLICATE KEY UPDATE
  quantity = VALUES(quantity),
  min_stock = VALUES(min_stock),
  max_stock = VALUES(max_stock),
  purchase_price = VALUES(purchase_price),
  selling_price = VALUES(selling_price),
  supplier = VALUES(supplier),
  batch_number = VALUES(batch_number),
  expiry_date = VALUES(expiry_date),
  update_time = NOW();

INSERT INTO medicine_stock_log (
  id, medicine_id, inventory_id, operation_type, quantity, before_stock,
  after_stock, unit_price, operator, remark, create_time
) VALUES
(9001, 1001, 9001, '入库', 200, 0, 200, 9.80, '1001', '首批入库', DATE_SUB(NOW(), INTERVAL 20 DAY)),
(9002, 1001, 9001, '出库', -20, 200, 180, 9.80, '1001', '处方发药', DATE_SUB(NOW(), INTERVAL 2 DAY)),
(9003, 1002, 9002, '入库', 80, 0, 80, 8.20, '1001', '首批入库', DATE_SUB(NOW(), INTERVAL 16 DAY)),
(9004, 1002, 9002, '出库', -20, 80, 60, 8.20, '1001', '门诊发药', DATE_SUB(NOW(), INTERVAL 1 DAY)),
(9005, 1003, 9003, '盘点调整', -6, 30, 24, 11.00, '1002', '盘点低库存预警', DATE_SUB(NOW(), INTERVAL 6 HOUR)),
(9006, 1004, 9004, '出库', -12, 20, 8, 6.50, '1002', '儿科发药后触发预警', DATE_SUB(NOW(), INTERVAL 4 HOUR))
ON DUPLICATE KEY UPDATE
  medicine_id = VALUES(medicine_id),
  inventory_id = VALUES(inventory_id),
  operation_type = VALUES(operation_type),
  quantity = VALUES(quantity),
  before_stock = VALUES(before_stock),
  after_stock = VALUES(after_stock),
  unit_price = VALUES(unit_price),
  operator = VALUES(operator),
  remark = VALUES(remark),
  create_time = VALUES(create_time);

-- =====================================================
-- Admin console data
-- =====================================================

INSERT INTO billing (
  id, patient_id, appointment_id, medical_record_id, item_type, item_name,
  amount, description, status, create_time, update_time
) VALUES
(9001, 'demo-patient-0001-0001-000000000001', 9001, NULL, 'REGISTRATION', '主任医师挂号费', 50.00, '心内科主任医师号', 1, DATE_SUB(NOW(), INTERVAL 2 DAY), NOW()),
(9002, 'demo-patient-0002-0002-000000000002', 9002, 9002, 'EXAMINATION', '心电图检查', 35.00, '胸闷及高血压评估', 1, DATE_SUB(NOW(), INTERVAL 1 DAY), NOW()),
(9003, 'demo-patient-0004-0004-000000000004', 9005, 9003, 'MEDICINE', '布洛芬缓释胶囊', 18.50, '骨科门诊处方用药', 1, DATE_SUB(NOW(), INTERVAL 3 DAY), NOW()),
(9004, 'demo-patient-0005-0005-000000000005', 9003, 9004, 'EXAMINATION', '心肌酶谱', 120.00, '胸闷待查', 0, NOW(), NOW()),
(9005, 'demo-patient-0003-0003-000000000003', 9004, NULL, 'REGISTRATION', '儿科普通号', 25.00, '儿科门诊挂号', 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE
  patient_id = VALUES(patient_id),
  appointment_id = VALUES(appointment_id),
  medical_record_id = VALUES(medical_record_id),
  item_type = VALUES(item_type),
  item_name = VALUES(item_name),
  amount = VALUES(amount),
  description = VALUES(description),
  status = VALUES(status),
  update_time = NOW();

INSERT INTO medical_report (
  id, patient_id, medical_record_id, doctor_id, report_type, title,
  examination_data, ai_summary, ai_diagnosis, ai_treatment, ai_recommendation,
  ai_thought_chain, pdf_path, status, create_time, update_time
) VALUES
(9001, 'demo-patient-0002-0002-000000000002', 9002, 1001, 'TREATMENT', '高血压随访报告', '血压 148/92mmHg，心率 76次/分。', '患者血压仍偏高，需要持续生活方式干预。', '原发性高血压 1 级', '低盐饮食、规律运动、家庭血压监测。', '两周后复诊，必要时启动药物治疗。', '基于主诉、血压记录和危险因素综合判断。', NULL, 1, DATE_SUB(NOW(), INTERVAL 13 DAY), DATE_SUB(NOW(), INTERVAL 13 DAY)),
(9002, 'demo-patient-0004-0004-000000000004', 9003, 1003, 'TREATMENT', '右膝扭伤治疗报告', '右膝轻度肿胀，活动受限。', '考虑运动损伤导致软组织扭伤。', '右膝关节扭伤', '休息制动、冷敷、必要时止痛。', '若疼痛加重或一周未缓解，建议复查影像。', '结合病史、体征和疼痛特点判断。', NULL, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY))
ON DUPLICATE KEY UPDATE
  patient_id = VALUES(patient_id),
  medical_record_id = VALUES(medical_record_id),
  doctor_id = VALUES(doctor_id),
  report_type = VALUES(report_type),
  title = VALUES(title),
  examination_data = VALUES(examination_data),
  ai_summary = VALUES(ai_summary),
  ai_diagnosis = VALUES(ai_diagnosis),
  ai_treatment = VALUES(ai_treatment),
  ai_recommendation = VALUES(ai_recommendation),
  ai_thought_chain = VALUES(ai_thought_chain),
  pdf_path = VALUES(pdf_path),
  status = VALUES(status),
  update_time = NOW();

INSERT INTO system_config (id, config_key, config_value, config_type, description, create_time, update_time) VALUES
(9001, 'hospital.name', '示例市第一人民医院', 'basic', '医院名称', NOW(), NOW()),
(9002, 'hospital.phone', '010-88886666', 'basic', '医院总机电话', NOW(), NOW()),
(9003, 'appointment.advance_days', '14', 'appointment', '可提前预约天数', NOW(), NOW()),
(9004, 'appointment.cancel_hours', '24', 'appointment', '预约取消最晚提前小时数', NOW(), NOW()),
(9005, 'inventory.low_stock_threshold', '30', 'inventory', '默认低库存预警阈值', NOW(), NOW()),
(9006, 'ai.triage.enabled', 'true', 'ai', '是否启用 AI 导诊', NOW(), NOW())
ON DUPLICATE KEY UPDATE
  config_key = VALUES(config_key),
  config_value = VALUES(config_value),
  config_type = VALUES(config_type),
  description = VALUES(description),
  update_time = NOW();

-- Quick checks for the three portals.
SELECT 'staff_portal_demo_data ready' AS message;
SELECT COUNT(*) AS doctor_today_queue
FROM appointment
WHERE doctor_id = 1001 AND appointment_date = CURDATE() AND status IN (0, 1, 2);
SELECT COUNT(*) AS pharmacist_pending_prescriptions
FROM prescription
WHERE status = 0;
SELECT COUNT(*) AS low_stock_medicines
FROM medicine_inventory
WHERE quantity <= min_stock;
