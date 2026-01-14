-- 更新 request_logs 表结构以支持完整的日志系统
ALTER TABLE request_logs
ADD COLUMN request_id VARCHAR(64) NOT NULL DEFAULT '' COMMENT '请求唯一ID' AFTER id,
ADD COLUMN request_type VARCHAR(32) NOT NULL DEFAULT 'chat_completions' COMMENT 'chat_completions / responses / messages' AFTER request_time,
ADD COLUMN is_stream TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否流式请求' AFTER actual_model_name,
ADD COLUMN first_token_ms INT DEFAULT 0 COMMENT '首token延迟(毫秒)' AFTER first_token_time,
ADD COLUMN total_time_ms INT DEFAULT 0 COMMENT '总耗时(毫秒)' AFTER totalTime,
ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS / FAIL' AFTER cost,
ADD COLUMN error_stage VARCHAR(32) NULL COMMENT 'CONNECT / HTTP / DECODE / TIMEOUT' AFTER errorMessage,
ADD COLUMN retry_count INT DEFAULT 0 COMMENT '故障转移次数' AFTER error_stage;

-- 添加索引
CREATE INDEX idx_request_id ON request_logs(request_id);
CREATE INDEX idx_provider ON request_logs(provider_id);
CREATE INDEX idx_time ON request_logs(request_time);

-- 修改现有字段注释
ALTER TABLE request_logs
MODIFY COLUMN request_time BIGINT NOT NULL COMMENT '请求时间戳（秒）',
MODIFY COLUMN first_token_time INT DEFAULT 0 COMMENT '首token时间戳（毫秒）',
MODIFY COLUMN totalTime INT DEFAULT 0 COMMENT '总耗时（毫秒）';