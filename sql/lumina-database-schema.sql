-- ============================================
-- Lumina (Octopus Java版) 数据库设计
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS lumina DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE lumina;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE `users` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 渠道表
-- ============================================
CREATE TABLE `channels` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '渠道ID',
    `name` VARCHAR(100) NOT NULL COMMENT '渠道名称',
    `type` TINYINT NOT NULL COMMENT '渠道类型：0-OpenAI Chat, 1-OpenAI Response, 2-Anthropic, 3-Gemini, 4-Volcengine',
    `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `base_url_list` JSON NOT NULL COMMENT 'Base URL列表，格式：[{"url":"https://api.openai.com/v1","delay":0}]',
    `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
    `custom_model_name` VARCHAR(100) DEFAULT NULL COMMENT '自定义模型名称',
    `use_proxy` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否使用代理：0-否，1-是',
    `auto_sync` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动同步：0-否，1-是',
    `auto_group_mode` TINYINT NOT NULL DEFAULT 0 COMMENT '自动分组模式：0-不自动，1-模糊，2-精确，3-正则',
    `custom_headers` JSON DEFAULT NULL COMMENT '自定义请求头，格式：{"key":"value"}',
    `param_override` TEXT DEFAULT NULL COMMENT '参数覆盖配置',
    `proxy_url` VARCHAR(255) DEFAULT NULL COMMENT '渠道专用代理地址',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`),
    KEY `idx_type` (`type`),
    KEY `idx_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道表';

-- ============================================
-- 3. 渠道密钥表
-- ============================================
CREATE TABLE `channel_keys` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '密钥ID',
    `channel_id` BIGINT UNSIGNED NOT NULL COMMENT '渠道ID',
    `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `api_key` VARCHAR(255) NOT NULL COMMENT 'API密钥',
    `status_code` INT DEFAULT NULL COMMENT '状态码（如429表示限流）',
    `last_used_at` BIGINT DEFAULT NULL COMMENT '最后使用时间戳（秒）',
    `total_cost` DECIMAL(10, 4) NOT NULL DEFAULT 0.0000 COMMENT '总消耗费用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_enabled` (`is_enabled`),
    CONSTRAINT `fk_channel_keys_channel` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道密钥表';

-- ============================================
-- 4. 分组表
-- ============================================
CREATE TABLE `groups` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    `name` VARCHAR(100) NOT NULL COMMENT '分组名称（对外暴露的模型名）',
    `balance_mode` TINYINT NOT NULL COMMENT '负载均衡模式：1-轮询，2-随机，3-故障转移，4-加权',
    `match_regex` VARCHAR(255) DEFAULT NULL COMMENT '匹配正则表达式',
    `first_token_timeout` INT DEFAULT NULL COMMENT '首个Token超时时间（秒）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分组表';

-- ============================================
-- 5. 分组项目表
-- ============================================
CREATE TABLE `group_items` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '项目ID',
    `group_id` BIGINT UNSIGNED NOT NULL COMMENT '分组ID',
    `channel_id` BIGINT UNSIGNED NOT NULL COMMENT '渠道ID',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
    `weight` INT NOT NULL DEFAULT 1 COMMENT '权重（用于加权负载均衡）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_group_channel_model` (`group_id`, `channel_id`, `model_name`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_channel_id` (`channel_id`),
    CONSTRAINT `fk_group_items_group` FOREIGN KEY (`group_id`) REFERENCES `groups` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_group_items_channel` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分组项目表';

-- ============================================
-- 6. API密钥表
-- ============================================
CREATE TABLE `api_keys` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'API密钥ID',
    `name` VARCHAR(100) NOT NULL COMMENT '密钥名称',
    `api_key` VARCHAR(255) NOT NULL COMMENT 'API密钥值',
    `is_enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    `expired_at` BIGINT DEFAULT NULL COMMENT '过期时间戳（秒），NULL表示永不过期',
    `max_amount` DECIMAL(10, 4) DEFAULT NULL COMMENT '最大消费额度，NULL表示无限制',
    `supported_models` TEXT DEFAULT NULL COMMENT '支持的模型列表（逗号分隔），NULL表示无限制',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_api_key` (`api_key`),
    KEY `idx_enabled` (`is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API密钥表';

-- ============================================
-- 7. 系统设置表
-- ============================================
CREATE TABLE `settings` (
    `setting_key` VARCHAR(100) NOT NULL COMMENT '设置键名',
    `setting_value` TEXT NOT NULL COMMENT '设置值',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`setting_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统设置表';

-- ============================================
-- 8. LLM模型信息表
-- ============================================
CREATE TABLE `llm_models` (
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `input_price` DECIMAL(10, 6) NOT NULL DEFAULT 0.000000 COMMENT '输入价格（每百万Token）',
    `output_price` DECIMAL(10, 6) NOT NULL DEFAULT 0.000000 COMMENT '输出价格（每百万Token）',
    `cache_read_price` DECIMAL(10, 6) NOT NULL DEFAULT 0.000000 COMMENT '缓存读取价格（每百万Token）',
    `cache_write_price` DECIMAL(10, 6) NOT NULL DEFAULT 0.000000 COMMENT '缓存写入价格（每百万Token）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM模型信息表';

-- ============================================
-- 9. 请求日志表
-- ============================================
CREATE TABLE `request_logs` (
    `id` BIGINT UNSIGNED NOT NULL COMMENT '日志ID（Snowflake ID）',
    `request_time` BIGINT NOT NULL COMMENT '请求时间戳（秒）',
    `request_model_name` VARCHAR(100) NOT NULL COMMENT '请求的模型名称',
    `channel_id` BIGINT UNSIGNED NOT NULL COMMENT '实际使用的渠道ID',
    `channel_name` VARCHAR(100) NOT NULL COMMENT '渠道名称',
    `actual_model_name` VARCHAR(100) NOT NULL COMMENT '实际使用的模型名称',
    `input_tokens` INT NOT NULL DEFAULT 0 COMMENT '输入Token数',
    `output_tokens` INT NOT NULL DEFAULT 0 COMMENT '输出Token数',
    `first_token_time` INT NOT NULL DEFAULT 0 COMMENT '首字时间（毫秒）',
    `total_time` INT NOT NULL DEFAULT 0 COMMENT '总用时（毫秒）',
    `cost` DECIMAL(10, 4) NOT NULL DEFAULT 0.0000 COMMENT '消耗费用',
    `request_content` MEDIUMTEXT DEFAULT NULL COMMENT '请求内容（JSON）',
    `response_content` MEDIUMTEXT DEFAULT NULL COMMENT '响应内容（JSON）',
    `error_message` TEXT DEFAULT NULL COMMENT '错误信息',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_request_time` (`request_time`),
    KEY `idx_channel_id` (`channel_id`),
    KEY `idx_request_model` (`request_model_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请求日志表';

-- ============================================
-- 10. 总统计表
-- ============================================
CREATE TABLE `stats_total` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '统计ID',
    `input_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输入Token总数',
    `output_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输出Token总数',
    `input_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输入成本',
    `output_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输出成本',
    `wait_time` BIGINT NOT NULL DEFAULT 0 COMMENT '等待时间（毫秒）',
    `request_success_count` BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    `request_failed_count` BIGINT NOT NULL DEFAULT 0 COMMENT '失败请求数',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='总统计表';

-- ============================================
-- 11. 小时统计表
-- ============================================
CREATE TABLE `stats_hourly` (
    `hour` TINYINT NOT NULL COMMENT '小时数（0-23）',
    `stat_date` VARCHAR(8) NOT NULL COMMENT '日期（格式：20260110）',
    `input_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输入Token总数',
    `output_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输出Token总数',
    `input_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输入成本',
    `output_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输出成本',
    `wait_time` BIGINT NOT NULL DEFAULT 0 COMMENT '等待时间（毫秒）',
    `request_success_count` BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    `request_failed_count` BIGINT NOT NULL DEFAULT 0 COMMENT '失败请求数',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`stat_date`, `hour`),
    KEY `idx_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小时统计表';

-- ============================================
-- 12. 日统计表
-- ============================================
CREATE TABLE `stats_daily` (
    `stat_date` VARCHAR(8) NOT NULL COMMENT '日期（格式：20260110）',
    `input_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输入Token总数',
    `output_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输出Token总数',
    `input_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输入成本',
    `output_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输出成本',
    `wait_time` BIGINT NOT NULL DEFAULT 0 COMMENT '等待时间（毫秒）',
    `request_success_count` BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    `request_failed_count` BIGINT NOT NULL DEFAULT 0 COMMENT '失败请求数',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日统计表';

-- ============================================
-- 13. 模型统计表
-- ============================================
CREATE TABLE `stats_model` (
    `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '统计ID',
    `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
    `channel_id` BIGINT UNSIGNED NOT NULL COMMENT '渠道ID',
    `input_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输入Token总数',
    `output_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输出Token总数',
    `input_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输入成本',
    `output_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输出成本',
    `wait_time` BIGINT NOT NULL DEFAULT 0 COMMENT '等待时间（毫秒）',
    `request_success_count` BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    `request_failed_count` BIGINT NOT NULL DEFAULT 0 COMMENT '失败请求数',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_model_channel` (`model_name`, `channel_id`),
    KEY `idx_model_name` (`model_name`),
    KEY `idx_channel_id` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模型统计表';

-- ============================================
-- 14. 渠道统计表
-- ============================================
CREATE TABLE `stats_channel` (
    `channel_id` BIGINT UNSIGNED NOT NULL COMMENT '渠道ID',
    `input_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输入Token总数',
    `output_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输出Token总数',
    `input_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输入成本',
    `output_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输出成本',
    `wait_time` BIGINT NOT NULL DEFAULT 0 COMMENT '等待时间（毫秒）',
    `request_success_count` BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    `request_failed_count` BIGINT NOT NULL DEFAULT 0 COMMENT '失败请求数',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`channel_id`),
    CONSTRAINT `fk_stats_channel` FOREIGN KEY (`channel_id`) REFERENCES `channels` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='渠道统计表';

-- ============================================
-- 15. API密钥统计表
-- ============================================
CREATE TABLE `stats_api_key` (
    `api_key_id` BIGINT UNSIGNED NOT NULL COMMENT 'API密钥ID',
    `input_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输入Token总数',
    `output_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '输出Token总数',
    `input_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输入成本',
    `output_cost` DECIMAL(12, 4) NOT NULL DEFAULT 0.0000 COMMENT '输出成本',
    `wait_time` BIGINT NOT NULL DEFAULT 0 COMMENT '等待时间（毫秒）',
    `request_success_count` BIGINT NOT NULL DEFAULT 0 COMMENT '成功请求数',
    `request_failed_count` BIGINT NOT NULL DEFAULT 0 COMMENT '失败请求数',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`api_key_id`),
    CONSTRAINT `fk_stats_api_key` FOREIGN KEY (`api_key_id`) REFERENCES `api_keys` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='API密钥统计表';

-- ============================================
-- 16. 数据库迁移记录表
-- ============================================
CREATE TABLE `migration_records` (
    `version` INT NOT NULL COMMENT '迁移版本号',
    `status` TINYINT NOT NULL COMMENT '迁移状态：1-成功，2-失败',
    `executed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    PRIMARY KEY (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据库迁移记录表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入默认管理员用户（密码：admin，BCrypt加密）
INSERT INTO `users` (`username`, `password`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH');

-- 插入默认系统设置
INSERT INTO `settings` (`setting_key`, `setting_value`) VALUES
('proxy_url', ''),
('stats_save_interval', '5'),
('model_info_update_interval', '24'),
('sync_llm_interval', '24'),
('relay_log_keep_period', '7'),
('relay_log_keep_enabled', '1'),
('cors_allow_origins', '*');

-- 初始化总统计表
INSERT INTO `stats_total` (`id`) VALUES (1);

-- ============================================
-- 索引优化建议
-- ============================================
-- 1. request_logs 表数据量大，建议按月分表
-- 2. stats_* 表建议定期归档历史数据
-- 3. 根据实际查询情况添加复合索引
-- 4. 考虑使用分区表优化大表查询性能
