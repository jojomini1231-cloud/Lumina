-- Add missing upstream fields to llm_models table
ALTER TABLE `llm_models` ADD COLUMN `display_name` TEXT DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `family` TEXT DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `is_attachment` INTEGER DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `is_structured_output` INTEGER DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `is_temperature` INTEGER DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `knowledge_cutoff` TEXT DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `release_date` TEXT DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `is_open_weights` INTEGER DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `output_type` TEXT DEFAULT NULL;
ALTER TABLE `llm_models` ADD COLUMN `input_limit` INTEGER DEFAULT NULL;
