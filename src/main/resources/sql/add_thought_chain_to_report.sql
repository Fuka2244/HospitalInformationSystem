-- 添加AI思维链字段到医疗报告表
-- 执行此SQL前请先备份数据库
-- 使用方法: mysql -u root -p his < add_thought_chain_to_report.sql

USE his;

-- 添加AI思维链字段（如果不存在）
ALTER TABLE `medical_report`
ADD COLUMN IF NOT EXISTS `ai_thought_chain` TEXT DEFAULT NULL COMMENT 'AI思维链分析过程' AFTER `ai_recommendation`;
