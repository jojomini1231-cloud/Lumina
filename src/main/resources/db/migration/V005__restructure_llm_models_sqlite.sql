-- Restructure llm_models to support multiple upstream providers per model
-- Add id column, is_active flag, and unique constraint on (model_name, provider)

ALTER TABLE `llm_models` ADD COLUMN `id` INTEGER PRIMARY KEY AUTOINCREMENT;
ALTER TABLE `llm_models` ADD COLUMN `is_active` INTEGER NOT NULL DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS `uk_model_provider` ON `llm_models` (`model_name`, `provider`);
UPDATE `llm_models` SET `is_active` = 1;
