-- Migration V002 (SQLite): Add stats aggregation tables for dashboard performance optimization
-- Date: 2026-05-12

CREATE TABLE IF NOT EXISTS stats_daily (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    stat_date TEXT NOT NULL,
    provider_id INTEGER DEFAULT NULL,
    provider_name TEXT DEFAULT NULL,
    model_name TEXT DEFAULT NULL,
    total_requests INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    total_input_tokens INTEGER NOT NULL DEFAULT 0,
    total_output_tokens INTEGER NOT NULL DEFAULT 0,
    total_cost REAL NOT NULL DEFAULT 0,
    total_latency_ms INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_daily_date_provider_model ON stats_daily (stat_date, provider_id, model_name);
CREATE INDEX IF NOT EXISTS idx_daily_stat_date ON stats_daily (stat_date);

CREATE TABLE IF NOT EXISTS stats_hourly (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    stat_hour TEXT NOT NULL,
    provider_id INTEGER DEFAULT NULL,
    provider_name TEXT DEFAULT NULL,
    model_name TEXT DEFAULT NULL,
    total_requests INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    total_input_tokens INTEGER NOT NULL DEFAULT 0,
    total_output_tokens INTEGER NOT NULL DEFAULT 0,
    total_cost REAL NOT NULL DEFAULT 0,
    total_latency_ms INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_hourly_hour_provider_model ON stats_hourly (stat_hour, provider_id, model_name);
CREATE INDEX IF NOT EXISTS idx_hourly_stat_hour ON stats_hourly (stat_hour);

CREATE INDEX IF NOT EXISTS idx_request_logs_created_at ON request_logs (created_at);
