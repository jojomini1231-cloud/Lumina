-- Migration V012: Add API key group
ALTER TABLE `api_keys` ADD COLUMN `key_group` varchar(100) NOT NULL DEFAULT '自用' COMMENT '密钥分组';
UPDATE `api_keys` SET `key_group` = '自用' WHERE `key_group` IS NULL OR TRIM(`key_group`) = '';
CREATE INDEX `idx_api_keys_key_group` ON `api_keys` (`key_group`);
