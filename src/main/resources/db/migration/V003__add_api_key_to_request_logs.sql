-- Add api_key column to request_logs for per-key usage tracking
ALTER TABLE `request_logs` ADD COLUMN `api_key` varchar(255) DEFAULT NULL COMMENT '客户端API密钥' AFTER `retry_count`;
CREATE INDEX `idx_request_logs_api_key` ON `request_logs` (`api_key`);
