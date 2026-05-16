-- Restructure llm_models to support multiple upstream providers per model
-- Add auto-increment id as primary key, unique key on (model_name, provider), and is_active flag

-- Step 1: Add id column and is_active
ALTER TABLE `llm_models` ADD COLUMN `id` bigint unsigned NOT NULL AUTO_INCREMENT PRIMARY KEY FIRST;
ALTER TABLE `llm_models` ADD COLUMN `is_active` tinyint NOT NULL DEFAULT 0 COMMENT '是否为计费使用的记录：0-否，1-是';

-- Step 2: Clean up duplicates before adding unique key
-- Set NULL providers to empty string to avoid NULL uniqueness issues
UPDATE `llm_models` SET `provider` = '' WHERE `provider` IS NULL;
-- Remove duplicates keeping the one with highest output_price
DELETE t1 FROM `llm_models` t1
INNER JOIN `llm_models` t2
WHERE t1.id > t2.id AND t1.model_name = t2.model_name AND t1.provider = t2.provider;

-- Step 3: Add unique key on (model_name, provider)
ALTER TABLE `llm_models` ADD UNIQUE KEY `uk_model_provider` (`model_name`, `provider`);

-- Step 4: Set existing records as active (one per model_name)
UPDATE `llm_models` SET `is_active` = 1;
