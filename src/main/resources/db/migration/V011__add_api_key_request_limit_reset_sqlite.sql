-- Migration V011: Add API key request limit reset timestamp for SQLite
ALTER TABLE `api_keys` ADD COLUMN `request_limit_reset_at` INTEGER DEFAULT NULL;
