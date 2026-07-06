-- Migration V010: Add API key request count limit
ALTER TABLE `api_keys` ADD COLUMN `max_requests` bigint DEFAULT NULL COMMENT '最大请求数，NULL表示无限制';
