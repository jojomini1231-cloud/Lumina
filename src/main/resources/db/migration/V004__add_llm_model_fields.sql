-- Add missing upstream fields to llm_models table
ALTER TABLE `llm_models` ADD COLUMN `display_name` varchar(200) DEFAULT NULL COMMENT '模型显示名称';
ALTER TABLE `llm_models` ADD COLUMN `family` varchar(100) DEFAULT NULL COMMENT '模型系列';
ALTER TABLE `llm_models` ADD COLUMN `is_attachment` tinyint DEFAULT NULL COMMENT '附件支持';
ALTER TABLE `llm_models` ADD COLUMN `is_structured_output` tinyint DEFAULT NULL COMMENT '结构化输出支持';
ALTER TABLE `llm_models` ADD COLUMN `is_temperature` tinyint DEFAULT NULL COMMENT '温度参数支持';
ALTER TABLE `llm_models` ADD COLUMN `knowledge_cutoff` varchar(25) DEFAULT NULL COMMENT '知识截止日期';
ALTER TABLE `llm_models` ADD COLUMN `release_date` varchar(25) DEFAULT NULL COMMENT '发布日期';
ALTER TABLE `llm_models` ADD COLUMN `is_open_weights` tinyint DEFAULT NULL COMMENT '开源权重';
ALTER TABLE `llm_models` ADD COLUMN `output_type` varchar(255) DEFAULT NULL COMMENT '输出类型支持';
ALTER TABLE `llm_models` ADD COLUMN `input_limit` int DEFAULT NULL COMMENT '输入限制';
