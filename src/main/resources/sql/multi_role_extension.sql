-- 1. 修改预约表，添加就诊地点和挂号登记状态
ALTER TABLE appointment 
ADD COLUMN location VARCHAR(100) COMMENT '就诊地点',
ADD COLUMN registration_status INT DEFAULT 0 COMMENT '挂号登记状态 0未登记 1已登记';

-- 2. 创建处方审核表
CREATE TABLE IF NOT EXISTS prescription_audit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    prescription_id BIGINT NOT NULL COMMENT '处方ID',
    pharmacist_id BIGINT NOT NULL COMMENT '药师ID',
    audit_status INT NOT NULL COMMENT '审核状态 0待审核 1通过 2拒绝',
    audit_remark VARCHAR(500) COMMENT '审核备注',
    audit_time DATETIME COMMENT '审核时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_prescription_id (prescription_id)
) COMMENT='处方审核记录表';

-- 3. 创建药品库存表
CREATE TABLE IF NOT EXISTS medicine_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    quantity INT NOT NULL DEFAULT 0 COMMENT '当前库存',
    min_stock INT DEFAULT 10 COMMENT '最小库存预警值',
    max_stock INT DEFAULT 1000 COMMENT '最大库存',
    purchase_price DECIMAL(10,2) COMMENT '进价',
    selling_price DECIMAL(10,2) COMMENT '售价',
    supplier VARCHAR(100) COMMENT '供应商',
    batch_number VARCHAR(50) COMMENT '批次号',
    expiry_date DATETIME COMMENT '有效期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_medicine_id (medicine_id)
) COMMENT='药品库存表';

-- 4. 创建检查单表
CREATE TABLE IF NOT EXISTS examination_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medical_record_id BIGINT COMMENT '病历ID',
    patient_id VARCHAR(50) NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    examination_name VARCHAR(100) NOT NULL COMMENT '检查项目名称',
    examination_type VARCHAR(50) COMMENT '检查类型',
    price DECIMAL(10,2) COMMENT '检查费用',
    status INT DEFAULT 0 COMMENT '状态 0未执行 1已执行 2已取消',
    result TEXT COMMENT '检查结果',
    examination_date DATETIME COMMENT '检查时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='检查单表';

-- 5. 创建就诊记录表
CREATE TABLE IF NOT EXISTS visit_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    appointment_id BIGINT NOT NULL COMMENT '预约ID',
    patient_id VARCHAR(50) NOT NULL COMMENT '患者ID',
    doctor_id BIGINT NOT NULL COMMENT '医生ID',
    department_id BIGINT COMMENT '科室ID',
    visit_number VARCHAR(20) COMMENT '就诊号',
    call_status INT DEFAULT 0 COMMENT '呼叫状态 0未呼叫 1已呼叫 2已过号',
    call_time DATETIME COMMENT '呼叫时间',
    check_in_time DATETIME COMMENT '签到时间',
    visit_start_time DATETIME COMMENT '就诊开始时间',
    visit_end_time DATETIME COMMENT '就诊结束时间',
    diagnosis TEXT COMMENT '诊断结果',
    treatment TEXT COMMENT '治疗方案',
    notes TEXT COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_appointment_id (appointment_id)
) COMMENT='就诊记录表';

-- 6. 创建系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL COMMENT '配置键',
    config_value VARCHAR(500) COMMENT '配置值',
    config_type VARCHAR(50) COMMENT '配置类型',
    description VARCHAR(200) COMMENT '描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_config_key (config_key)
) COMMENT='系统配置表';

-- 7. 创建药品库存变动日志表
CREATE TABLE IF NOT EXISTS medicine_stock_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    medicine_id BIGINT NOT NULL COMMENT '药品ID',
    inventory_id BIGINT COMMENT '库存记录ID',
    operation_type VARCHAR(20) NOT NULL COMMENT '操作类型 入库/出库/盘点/损耗',
    quantity INT NOT NULL COMMENT '变动数量（正数为增加，负数为减少）',
    before_stock INT COMMENT '变动前库存',
    after_stock INT COMMENT '变动后库存',
    unit_price DECIMAL(10,2) COMMENT '单价',
    operator VARCHAR(50) COMMENT '操作人',
    remark VARCHAR(200) COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_medicine_id (medicine_id)
) COMMENT='药品库存变动日志表';

-- 8. 初始化系统配置数据
INSERT INTO system_config (config_key, config_value, config_type, description) VALUES
('hospital.name', 'XX医院', 'basic', '医院名称'),
('hospital.address', 'XX省XX市XX区XX路XX号', 'basic', '医院地址'),
('hospital.phone', '010-12345678', 'basic', '医院电话'),
('appointment.advance_days', '7', 'appointment', '预约提前天数'),
('appointment.cancel_hours', '24', 'appointment', '取消预约提前小时数'),
('inventory.low_stock_threshold', '10', 'inventory', '低库存预警阈值')
ON DUPLICATE KEY UPDATE config_value=VALUES(config_value);