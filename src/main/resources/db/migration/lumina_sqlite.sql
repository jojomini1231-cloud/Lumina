/*
 Lumina SQLite Database Schema (Complete)
 Version: 0.4.0
 Includes all migrations through V005

 Usage:
   sqlite3 lumina.db < lumina_sqlite.sql
*/

PRAGMA foreign_keys = ON;

-- ----------------------------
-- Table: users
-- ----------------------------
CREATE TABLE IF NOT EXISTS `users` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `username` TEXT NOT NULL UNIQUE,
  `password` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT (datetime('now')),
  `updated_at` DATETIME NOT NULL DEFAULT (datetime('now'))
);

-- ----------------------------
-- Table: settings
-- ----------------------------
CREATE TABLE IF NOT EXISTS `settings` (
  `setting_key` TEXT PRIMARY KEY,
  `setting_value` TEXT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT (datetime('now')),
  `updated_at` DATETIME NOT NULL DEFAULT (datetime('now'))
);

-- ----------------------------
-- Table: api_keys
-- ----------------------------
CREATE TABLE IF NOT EXISTS `api_keys` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `name` TEXT NOT NULL,
  `api_key` TEXT NOT NULL UNIQUE,
  `is_enabled` INTEGER NOT NULL DEFAULT 1,
  `expired_at` INTEGER,
  `max_amount` REAL,
  `supported_models` TEXT,
  `created_at` DATETIME NOT NULL DEFAULT (datetime('now')),
  `updated_at` DATETIME NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS `idx_api_keys_enabled` ON `api_keys` (`is_enabled`);

-- ----------------------------
-- Table: providers
-- ----------------------------
CREATE TABLE IF NOT EXISTS `providers` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `name` TEXT NOT NULL DEFAULT '' UNIQUE,
  `type` INTEGER NOT NULL DEFAULT 0,
  `base_url` TEXT NOT NULL DEFAULT '',
  `api_key` TEXT NOT NULL DEFAULT '',
  `model_name` TEXT NOT NULL DEFAULT '',
  `actual_model` TEXT,
  `beta` INTEGER DEFAULT 0,
  `auto_sync` INTEGER NOT NULL DEFAULT 0,
  `is_enabled` INTEGER NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT (datetime('now')),
  `updated_at` DATETIME NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS `idx_providers_type` ON `providers` (`type`);
CREATE INDEX IF NOT EXISTS `idx_providers_enabled` ON `providers` (`is_enabled`);

-- ----------------------------
-- Table: model_groups
-- ----------------------------
CREATE TABLE IF NOT EXISTS `model_groups` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `name` TEXT NOT NULL UNIQUE,
  `balance_mode` INTEGER NOT NULL,
  `match_regex` TEXT,
  `first_token_timeout` INTEGER DEFAULT 45000,
  `created_at` DATETIME NOT NULL DEFAULT (datetime('now')),
  `updated_at` DATETIME NOT NULL DEFAULT (datetime('now'))
);

-- ----------------------------
-- Table: model_group_items
-- ----------------------------
CREATE TABLE IF NOT EXISTS `model_group_items` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `group_id` INTEGER NOT NULL,
  `provider_id` INTEGER NOT NULL,
  `model_name` TEXT NOT NULL,
  `priority` INTEGER NOT NULL DEFAULT 0,
  `weight` INTEGER NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT (datetime('now')),
  `updated_at` DATETIME NOT NULL DEFAULT (datetime('now')),
  FOREIGN KEY (`group_id`) REFERENCES `model_groups` (`id`) ON DELETE CASCADE,
  FOREIGN KEY (`provider_id`) REFERENCES `providers` (`id`) ON DELETE CASCADE,
  UNIQUE (`group_id`, `provider_id`, `model_name`)
);

CREATE INDEX IF NOT EXISTS `idx_group_items_group_id` ON `model_group_items` (`group_id`);
CREATE INDEX IF NOT EXISTS `idx_group_items_provider_id` ON `model_group_items` (`provider_id`);

-- ----------------------------
-- Table: llm_models (includes V004, V005)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `llm_models` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `model_name` TEXT NOT NULL,
  `provider` TEXT NOT NULL DEFAULT '',
  `input_price` REAL DEFAULT 0.0,
  `output_price` REAL DEFAULT 0.0,
  `context_limit` INTEGER DEFAULT 0,
  `output_limit` INTEGER DEFAULT 0,
  `cache_read_price` REAL,
  `cache_write_price` REAL,
  `is_reasoning` INTEGER,
  `is_tool_call` INTEGER,
  `is_attachment` INTEGER,
  `is_structured_output` INTEGER,
  `is_temperature` INTEGER,
  `is_open_weights` INTEGER,
  `input_type` TEXT,
  `output_type` TEXT,
  `display_name` TEXT,
  `family` TEXT,
  `knowledge_cutoff` TEXT,
  `release_date` TEXT,
  `input_limit` INTEGER,
  `last_updated_at` TEXT,
  `is_active` INTEGER NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT (datetime('now')),
  `updated_at` DATETIME NOT NULL DEFAULT (datetime('now'))
);

CREATE UNIQUE INDEX IF NOT EXISTS `uk_model_provider` ON `llm_models` (`model_name`, `provider`);

-- ----------------------------
-- Table: provider_runtime_stats
-- ----------------------------
CREATE TABLE IF NOT EXISTS `provider_runtime_stats` (
  `provider_id` TEXT PRIMARY KEY,
  `provider_name` TEXT,
  `success_rate_ema` REAL NOT NULL DEFAULT 1,
  `latency_ema_ms` REAL NOT NULL DEFAULT 0,
  `score` REAL NOT NULL DEFAULT 100,
  `total_requests` INTEGER NOT NULL DEFAULT 0,
  `success_requests` INTEGER NOT NULL DEFAULT 0,
  `failure_requests` INTEGER NOT NULL DEFAULT 0,
  `circuit_state` TEXT NOT NULL DEFAULT 'CLOSED',
  `circuit_opened_at` INTEGER NOT NULL DEFAULT 0,
  `consecutive_failures` INTEGER DEFAULT NULL,
  `open_attempt` INTEGER DEFAULT NULL,
  `next_probe_at` INTEGER DEFAULT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS `idx_provider_stats_score` ON `provider_runtime_stats` (`score`);
CREATE INDEX IF NOT EXISTS `idx_provider_stats_circuit` ON `provider_runtime_stats` (`circuit_state`);

-- ----------------------------
-- Table: request_logs
-- ----------------------------
CREATE TABLE IF NOT EXISTS `request_logs` (
  `id` TEXT PRIMARY KEY,
  `request_id` TEXT DEFAULT '',
  `request_time` INTEGER,
  `request_type` TEXT DEFAULT 'chat_completions',
  `request_model_name` TEXT,
  `provider_id` INTEGER,
  `provider_name` TEXT,
  `actual_model_name` TEXT,
  `is_stream` INTEGER DEFAULT 0,
  `input_tokens` INTEGER DEFAULT 0,
  `output_tokens` INTEGER DEFAULT 0,
  `first_token_time` INTEGER DEFAULT 0,
  `first_token_ms` INTEGER DEFAULT 0,
  `total_time` INTEGER DEFAULT 0,
  `total_time_ms` INTEGER DEFAULT 0,
  `cost` REAL DEFAULT 0.0,
  `status` TEXT DEFAULT 'SUCCESS',
  `request_content` TEXT,
  `response_content` TEXT,
  `error_message` TEXT,
  `error_stage` TEXT,
  `retry_count` INTEGER DEFAULT 0,
  `api_key` TEXT DEFAULT NULL,
  `created_at` DATETIME DEFAULT (datetime('now'))
);

CREATE INDEX IF NOT EXISTS `idx_logs_request_time` ON `request_logs` (`request_time`);
CREATE INDEX IF NOT EXISTS `idx_logs_provider_id` ON `request_logs` (`provider_id`);
CREATE INDEX IF NOT EXISTS `idx_logs_request_model` ON `request_logs` (`request_model_name`);
CREATE INDEX IF NOT EXISTS `idx_logs_request_id` ON `request_logs` (`request_id`);
CREATE INDEX IF NOT EXISTS `idx_logs_api_key` ON `request_logs` (`api_key`);
CREATE INDEX IF NOT EXISTS `idx_logs_created_at` ON `request_logs` (`created_at`);

-- ----------------------------
-- Table: stats_daily (V002)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stats_daily` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `stat_date` TEXT NOT NULL,
  `provider_id` INTEGER DEFAULT NULL,
  `provider_name` TEXT DEFAULT NULL,
  `model_name` TEXT DEFAULT NULL,
  `total_requests` INTEGER NOT NULL DEFAULT 0,
  `success_count` INTEGER NOT NULL DEFAULT 0,
  `total_input_tokens` INTEGER NOT NULL DEFAULT 0,
  `total_output_tokens` INTEGER NOT NULL DEFAULT 0,
  `total_cost` REAL NOT NULL DEFAULT 0,
  `total_latency_ms` INTEGER NOT NULL DEFAULT 0,
  `created_at` TEXT NOT NULL DEFAULT (datetime('now')),
  `updated_at` TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE UNIQUE INDEX IF NOT EXISTS `uk_daily_date_provider_model` ON `stats_daily` (`stat_date`, `provider_id`, `model_name`);
CREATE INDEX IF NOT EXISTS `idx_daily_stat_date` ON `stats_daily` (`stat_date`);

-- ----------------------------
-- Table: stats_hourly (V002)
-- ----------------------------
CREATE TABLE IF NOT EXISTS `stats_hourly` (
  `id` INTEGER PRIMARY KEY AUTOINCREMENT,
  `stat_hour` TEXT NOT NULL,
  `provider_id` INTEGER DEFAULT NULL,
  `provider_name` TEXT DEFAULT NULL,
  `model_name` TEXT DEFAULT NULL,
  `total_requests` INTEGER NOT NULL DEFAULT 0,
  `success_count` INTEGER NOT NULL DEFAULT 0,
  `total_input_tokens` INTEGER NOT NULL DEFAULT 0,
  `total_output_tokens` INTEGER NOT NULL DEFAULT 0,
  `total_cost` REAL NOT NULL DEFAULT 0,
  `total_latency_ms` INTEGER NOT NULL DEFAULT 0,
  `created_at` TEXT NOT NULL DEFAULT (datetime('now')),
  `updated_at` TEXT NOT NULL DEFAULT (datetime('now'))
);

CREATE UNIQUE INDEX IF NOT EXISTS `uk_hourly_hour_provider_model` ON `stats_hourly` (`stat_hour`, `provider_id`, `model_name`);
CREATE INDEX IF NOT EXISTS `idx_hourly_stat_hour` ON `stats_hourly` (`stat_hour`);

-- ----------------------------
-- Table: migration_records
-- ----------------------------
CREATE TABLE IF NOT EXISTS `migration_records` (
  `version` INTEGER PRIMARY KEY,
  `status` INTEGER NOT NULL,
  `executed_at` DATETIME NOT NULL DEFAULT (datetime('now'))
);

-- ----------------------------
-- Triggers for auto-update timestamp
-- ----------------------------
CREATE TRIGGER IF NOT EXISTS `trg_users_updated`
AFTER UPDATE ON `users` FOR EACH ROW
BEGIN UPDATE `users` SET `updated_at` = datetime('now') WHERE `id` = NEW.`id`; END;

CREATE TRIGGER IF NOT EXISTS `trg_settings_updated`
AFTER UPDATE ON `settings` FOR EACH ROW
BEGIN UPDATE `settings` SET `updated_at` = datetime('now') WHERE `setting_key` = NEW.`setting_key`; END;

CREATE TRIGGER IF NOT EXISTS `trg_api_keys_updated`
AFTER UPDATE ON `api_keys` FOR EACH ROW
BEGIN UPDATE `api_keys` SET `updated_at` = datetime('now') WHERE `id` = NEW.`id`; END;

CREATE TRIGGER IF NOT EXISTS `trg_providers_updated`
AFTER UPDATE ON `providers` FOR EACH ROW
BEGIN UPDATE `providers` SET `updated_at` = datetime('now') WHERE `id` = NEW.`id`; END;

CREATE TRIGGER IF NOT EXISTS `trg_model_groups_updated`
AFTER UPDATE ON `model_groups` FOR EACH ROW
BEGIN UPDATE `model_groups` SET `updated_at` = datetime('now') WHERE `id` = NEW.`id`; END;

CREATE TRIGGER IF NOT EXISTS `trg_group_items_updated`
AFTER UPDATE ON `model_group_items` FOR EACH ROW
BEGIN UPDATE `model_group_items` SET `updated_at` = datetime('now') WHERE `id` = NEW.`id`; END;

CREATE TRIGGER IF NOT EXISTS `trg_llm_models_updated`
AFTER UPDATE ON `llm_models` FOR EACH ROW
BEGIN UPDATE `llm_models` SET `updated_at` = datetime('now') WHERE `id` = NEW.`id`; END;

CREATE TRIGGER IF NOT EXISTS `trg_runtime_stats_updated`
AFTER UPDATE ON `provider_runtime_stats` FOR EACH ROW
BEGIN UPDATE `provider_runtime_stats` SET `updated_at` = datetime('now') WHERE `provider_id` = NEW.`provider_id`; END;
