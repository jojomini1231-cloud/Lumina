-- Migration V002: Add stats aggregation tables for dashboard performance optimization
-- Date: 2026-05-12

-- 按天聚合统计表（用于 overview、provider-stats）
CREATE TABLE IF NOT EXISTS stats_daily (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    stat_date DATE NOT NULL COMMENT '统计日期',
    provider_id BIGINT UNSIGNED DEFAULT NULL COMMENT '供应商ID，NULL表示全局汇总',
    provider_name VARCHAR(100) DEFAULT NULL COMMENT '供应商名称',
    model_name VARCHAR(100) DEFAULT NULL COMMENT '模型名称，NULL表示供应商级汇总',
    total_requests BIGINT NOT NULL DEFAULT 0 COMMENT '总请求数',
    success_count BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    total_input_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '总输入Token数',
    total_output_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '总输出Token数',
    total_cost DECIMAL(14,4) NOT NULL DEFAULT 0 COMMENT '总费用',
    total_latency_ms BIGINT NOT NULL DEFAULT 0 COMMENT '总延迟毫秒数（用于计算平均值）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_date_provider_model (stat_date, provider_id, model_name),
    KEY idx_stat_date (stat_date),
    KEY idx_provider_id (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='按天聚合统计表';

-- 按小时聚合统计表（用于 traffic 图表、model-token-usage）
CREATE TABLE IF NOT EXISTS stats_hourly (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    stat_hour DATETIME NOT NULL COMMENT '统计小时（精确到小时，如 2026-05-12 14:00:00）',
    provider_id BIGINT UNSIGNED DEFAULT NULL COMMENT '供应商ID，NULL表示全局汇总',
    provider_name VARCHAR(100) DEFAULT NULL COMMENT '供应商名称',
    model_name VARCHAR(100) DEFAULT NULL COMMENT '模型名称，NULL表示供应商级汇总',
    total_requests BIGINT NOT NULL DEFAULT 0 COMMENT '总请求数',
    success_count BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    total_input_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '总输入Token数',
    total_output_tokens BIGINT NOT NULL DEFAULT 0 COMMENT '总输出Token数',
    total_cost DECIMAL(14,4) NOT NULL DEFAULT 0 COMMENT '总费用',
    total_latency_ms BIGINT NOT NULL DEFAULT 0 COMMENT '总延迟毫秒数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_hour_provider_model (stat_hour, provider_id, model_name),
    KEY idx_stat_hour (stat_hour),
    KEY idx_provider_id (provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='按小时聚合统计表';

-- 补充 request_logs.created_at 索引（当前缺失，影响按时间范围查询性能）
CREATE INDEX idx_created_at ON request_logs (created_at);
