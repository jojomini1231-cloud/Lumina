-- Migration V013: Add API key concurrent request limit
ALTER TABLE `api_keys` ADD COLUMN `max_concurrent_requests` bigint DEFAULT NULL COMMENT '最大API请求并发量，NULL表示无限制';
