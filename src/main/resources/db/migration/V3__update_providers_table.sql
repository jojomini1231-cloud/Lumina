-- 更新 providers 表结构
ALTER TABLE providers
ADD COLUMN actual_model VARCHAR(100) NULL COMMENT '实际模型名' AFTER model_name,
ADD COLUMN beta BOOLEAN DEFAULT FALSE COMMENT '是否使用beta功能' AFTER actual_model;