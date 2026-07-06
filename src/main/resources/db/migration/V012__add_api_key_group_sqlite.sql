-- Migration V012: Add API key group for SQLite
ALTER TABLE `api_keys` ADD COLUMN `key_group` TEXT NOT NULL DEFAULT '自用';
UPDATE `api_keys` SET `key_group` = '自用' WHERE `key_group` IS NULL OR TRIM(`key_group`) = '';
CREATE INDEX IF NOT EXISTS `idx_api_keys_key_group` ON `api_keys` (`key_group`);
