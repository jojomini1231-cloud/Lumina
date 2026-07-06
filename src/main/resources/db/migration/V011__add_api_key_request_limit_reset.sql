-- Migration V011: Add API key request limit reset timestamp
ALTER TABLE `api_keys` ADD COLUMN `request_limit_reset_at` bigint DEFAULT NULL COMMENT '请求数限制重置时间戳（秒），NULL表示未重置';
