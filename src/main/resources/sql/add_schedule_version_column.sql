-- 预约系统优化：添加乐观锁版本号字段
-- 执行前请备份数据库

-- 为doctor_schedule表添加version字段（乐观锁）
ALTER TABLE doctor_schedule ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';

-- 为现有记录初始化版本号
UPDATE doctor_schedule SET version = 1 WHERE version IS NULL OR version = 0;

-- 添加索引优化查询
CREATE INDEX idx_schedule_query ON doctor_schedule (doctor_id, schedule_date, time_slot);

-- 可选：添加定时任务调用接口的记录（用于监控）
-- INSERT INTO sys_schedule_job (job_name, job_type, cron_expression, status, remark)
-- VALUES ('sync_stock_to_redis', 'HTTP', '0 0 2 * * ?', 1, '每日凌晨2点同步号源库存到Redis');
