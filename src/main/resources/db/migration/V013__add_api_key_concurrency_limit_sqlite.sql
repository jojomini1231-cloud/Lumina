-- Migration V013: Add API key concurrent request limit for SQLite
ALTER TABLE `api_keys` ADD COLUMN `max_concurrent_requests` INTEGER DEFAULT NULL;
