-- Migration V010: Add API key request count limit for SQLite
ALTER TABLE `api_keys` ADD COLUMN `max_requests` INTEGER DEFAULT NULL;
