-- Add api_key column to request_logs for per-key usage tracking
ALTER TABLE `request_logs` ADD COLUMN `api_key` TEXT DEFAULT NULL;
CREATE INDEX IF NOT EXISTS `idx_request_logs_api_key` ON `request_logs` (`api_key`);
