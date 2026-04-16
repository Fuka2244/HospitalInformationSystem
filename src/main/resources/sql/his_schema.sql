-- =====================================================
-- 医院信息系统 (HIS) 数据库建表脚本
-- Database: his
-- =====================================================

-- 患者表（已存在，此处补充完整定义）
CREATE TABLE IF NOT EXISTS `patient` (
    `account`   VARCHAR(36)  NOT NULL COMMENT '账户ID(UUID)',
    `username`  VARCHAR(50)  DEFAULT NULL COMMENT '用户名',
    `name`      VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
    `password`  VARCHAR(100) DEFAULT NULL COMMENT '密码(BCrypt)',
    `gender`    VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `age`       INT          DEFAULT NULL COMMENT '年龄',
    `id_card`   VARCHAR(18)  DEFAULT NULL COMMENT '身份证号',
    `phone`     VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    `address`   VARCHAR(200) DEFAULT NULL COMMENT '地址',
    PRIMARY KEY (`account`),
    UNIQUE KEY `uk_phone` (`phone`),
    UNIQUE KEY `uk_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='患者信息表';

-- 科室表
CREATE TABLE IF NOT EXISTS `department` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '科室ID',
    `name`        VARCHAR(100) NOT NULL COMMENT '科室名称',
    `description` TEXT         DEFAULT NULL COMMENT '科室描述',
    `location`    VARCHAR(200) DEFAULT NULL COMMENT '科室位置',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1启用,0停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='科室表';

-- 医生表
CREATE TABLE IF NOT EXISTS `doctor` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '医生ID',
    `name`          VARCHAR(50)  NOT NULL COMMENT '姓名',
    `gender`        VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `age`           INT          DEFAULT NULL COMMENT '年龄',
    `title`         VARCHAR(50)  DEFAULT NULL COMMENT '职称(主任医师/副主任医师/主治医师/住院医师)',
    `department_id` BIGINT       NOT NULL COMMENT '所属科室ID',
    `specialty`     VARCHAR(500) DEFAULT NULL COMMENT '擅长领域',
    `phone`         VARCHAR(11)  DEFAULT NULL COMMENT '联系电话',
    `status`        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1在岗,0离岗',
    `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_department_id` (`department_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生表';

-- 管理员表
CREATE TABLE IF NOT EXISTS `admin` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    `name`        VARCHAR(50)  DEFAULT NULL COMMENT '姓名',
    `phone`       VARCHAR(11)  DEFAULT NULL COMMENT '手机号',
    `role`        VARCHAR(20)  NOT NULL DEFAULT 'ADMIN' COMMENT '角色',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 药师表
CREATE TABLE IF NOT EXISTS `pharmacist` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '药师ID',
    `name`        VARCHAR(50)  NOT NULL COMMENT '姓名',
    `gender`      VARCHAR(10)  DEFAULT NULL COMMENT '性别',
    `phone`       VARCHAR(11)  DEFAULT NULL COMMENT '联系电话',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态:1在岗,0离岗',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药师表';

-- 电子病历表
CREATE TABLE IF NOT EXISTS `medical_record` (
    `id`            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '病历ID',
    `patient_id`    VARCHAR(36) NOT NULL COMMENT '患者账户ID',
    `doctor_id`     BIGINT   NOT NULL COMMENT '医生ID',
    `department_id` BIGINT   DEFAULT NULL COMMENT '科室ID',
    `chief_complaint` TEXT    DEFAULT NULL COMMENT '主诉',
    `present_illness` TEXT    DEFAULT NULL COMMENT '现病史',
    `diagnosis`     TEXT     DEFAULT NULL COMMENT '诊断结果',
    `treatment_plan` TEXT    DEFAULT NULL COMMENT '治疗方案',
    `visit_date`    DATETIME NOT NULL COMMENT '就诊时间',
    `status`        TINYINT  NOT NULL DEFAULT 1 COMMENT '状态:1有效,0作废',
    `create_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_doctor_id` (`doctor_id`),
    KEY `idx_visit_date` (`visit_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电子病历表';

-- 处方表
CREATE TABLE IF NOT EXISTS `prescription` (
    `id`              BIGINT   NOT NULL AUTO_INCREMENT COMMENT '处方ID',
    `medical_record_id` BIGINT NOT NULL COMMENT '病历ID',
    `patient_id`      VARCHAR(36) NOT NULL COMMENT '患者账户ID',
    `doctor_id`       BIGINT   NOT NULL COMMENT '医生ID',
    `prescription_date` DATETIME NOT NULL COMMENT '开方日期',
    `status`          TINYINT  NOT NULL DEFAULT 0 COMMENT '状态:0待取药,1已取药,2已取消',
    `create_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_medical_record_id` (`medical_record_id`),
    KEY `idx_patient_id` (`patient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方表';

-- 处方明细表
CREATE TABLE IF NOT EXISTS `prescription_item` (
    `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细ID',
    `prescription_id` BIGINT NOT NULL COMMENT '处方ID',
    `medicine_id`     BIGINT NOT NULL COMMENT '药品ID',
    `dosage`          VARCHAR(100) DEFAULT NULL COMMENT '用法用量',
    `quantity`        INT    NOT NULL DEFAULT 1 COMMENT '数量',
    `days`            INT    DEFAULT NULL COMMENT '天数',
    `remark`          VARCHAR(200) DEFAULT NULL COMMENT '备注',
    PRIMARY KEY (`id`),
    KEY `idx_prescription_id` (`prescription_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='处方明细表';

-- 药品表
CREATE TABLE IF NOT EXISTS `medicine` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '药品ID',
    `name`          VARCHAR(100)  NOT NULL COMMENT '药品名称',
    `generic_name`  VARCHAR(100)  DEFAULT NULL COMMENT '通用名',
    `category`      VARCHAR(50)   DEFAULT NULL COMMENT '分类(中成药/化学药/生物制品)',
    `specification` VARCHAR(100)  DEFAULT NULL COMMENT '规格',
    `manufacturer`  VARCHAR(200)  DEFAULT NULL COMMENT '生产厂家',
    `ingredients`   TEXT          DEFAULT NULL COMMENT '成分',
    `efficacy`      TEXT          DEFAULT NULL COMMENT '功效/适应症',
    `side_effects`  TEXT          DEFAULT NULL COMMENT '副作用/不良反应',
    `contraindications` TEXT     DEFAULT NULL COMMENT '禁忌',
    `price`         DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '价格',
    `stock`         INT           NOT NULL DEFAULT 0 COMMENT '库存',
    `status`        TINYINT       NOT NULL DEFAULT 1 COMMENT '状态:1在售,0停售',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='药品表';

-- 预约表
CREATE TABLE IF NOT EXISTS `appointment` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '预约ID',
    `patient_id`      VARCHAR(36)  NOT NULL COMMENT '患者账户ID',
    `doctor_id`       BIGINT       DEFAULT NULL COMMENT '医生ID',
    `department_id`   BIGINT       NOT NULL COMMENT '科室ID',
    `appointment_type` VARCHAR(20) NOT NULL DEFAULT 'DOCTOR' COMMENT '预约类型:DOCTOR医生/EXAMINATION检查',
    `examination_type` VARCHAR(50) DEFAULT NULL COMMENT '检查类型(CT/MRI等)',
    `appointment_date` DATE        NOT NULL COMMENT '预约日期',
    `time_slot`       VARCHAR(50)  NOT NULL COMMENT '时间段(如 09:00-10:00)',
    `status`          TINYINT      NOT NULL DEFAULT 0 COMMENT '状态:0已预约,1已完成,2已取消',
    `cancel_reason`   VARCHAR(200) DEFAULT NULL COMMENT '取消原因',
    `ai_recommended`  TINYINT      NOT NULL DEFAULT 0 COMMENT '是否AI推荐:0否,1是',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_doctor_id` (`doctor_id`),
    KEY `idx_appointment_date` (`appointment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预约表';

-- 费用表
CREATE TABLE IF NOT EXISTS `billing` (
    `id`              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '费用ID',
    `patient_id`      VARCHAR(36)   NOT NULL COMMENT '患者账户ID',
    `appointment_id`  BIGINT        DEFAULT NULL COMMENT '关联预约ID',
    `medical_record_id` BIGINT      DEFAULT NULL COMMENT '关联病历ID',
    `item_type`       VARCHAR(30)   NOT NULL COMMENT '费用类型:REGISTRATION挂号/EXAMINATION检查/MEDICINE药品/OTHER其他',
    `item_name`       VARCHAR(200)  NOT NULL COMMENT '项目名称',
    `amount`          DECIMAL(10,2) NOT NULL COMMENT '金额',
    `description`     VARCHAR(500)  DEFAULT NULL COMMENT '说明',
    `status`          TINYINT       NOT NULL DEFAULT 0 COMMENT '状态:0未支付,1已支付,2已退款',
    `create_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_item_type` (`item_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='费用表';

-- 医疗报告表
CREATE TABLE IF NOT EXISTS `medical_report` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT COMMENT '报告ID',
    `patient_id`       VARCHAR(36) NOT NULL COMMENT '患者账户ID',
    `medical_record_id` BIGINT  DEFAULT NULL COMMENT '关联病历ID',
    `doctor_id`        BIGINT   NOT NULL COMMENT '医生ID',
    `report_type`      VARCHAR(50) NOT NULL COMMENT '报告类型:EXAMINATION检查报告/TREATMENT治疗报告',
    `title`            VARCHAR(200) DEFAULT NULL COMMENT '报告标题',
    `examination_data` TEXT     DEFAULT NULL COMMENT '检查数据/结果',
    `ai_summary`       TEXT     DEFAULT NULL COMMENT 'AI生成摘要',
    `ai_diagnosis`     TEXT     DEFAULT NULL COMMENT 'AI生成诊断',
    `ai_treatment`     TEXT     DEFAULT NULL COMMENT 'AI生成治疗方案',
    `ai_recommendation` TEXT    DEFAULT NULL COMMENT 'AI生成建议',
    `pdf_path`         VARCHAR(500) DEFAULT NULL COMMENT 'PDF文件路径',
    `status`           TINYINT  NOT NULL DEFAULT 0 COMMENT '状态:0草稿,1已确认,2已作废',
    `create_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_patient_id` (`patient_id`),
    KEY `idx_medical_record_id` (`medical_record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医疗报告表';

-- 医生出诊排班表
CREATE TABLE IF NOT EXISTS `doctor_schedule` (
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '排班ID',
    `doctor_id`   BIGINT      NOT NULL COMMENT '医生ID',
    `schedule_date` DATE      NOT NULL COMMENT '排班日期',
    `time_slot`   VARCHAR(50) NOT NULL COMMENT '时间段',
    `max_patients` INT        NOT NULL DEFAULT 20 COMMENT '最大接诊数',
    `booked_count` INT        NOT NULL DEFAULT 0 COMMENT '已预约数',
    `status`      TINYINT     NOT NULL DEFAULT 1 COMMENT '状态:1可预约,0停诊',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doctor_date_slot` (`doctor_id`, `schedule_date`, `time_slot`),
    KEY `idx_schedule_date` (`schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医生排班表';

-- 添加AI思维链字段到医疗报告表
ALTER TABLE `medical_report` ADD COLUMN `ai_thought_chain` TEXT DEFAULT NULL COMMENT 'AI思维链' AFTER `ai_recommendation`;
