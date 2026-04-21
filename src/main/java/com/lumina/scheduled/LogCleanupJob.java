package com.lumina.scheduled;

import com.lumina.config.LuminaProperties;
import com.lumina.service.RequestLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * 日志定期清理任务
 * 根据 lumina.stats.log-keep-days 配置定期清理过期日志
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanupJob {

    private final RequestLogService requestLogService;
    private final LuminaProperties luminaProperties;

    /**
     * 每天凌晨 3 点执行一次清理
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredLogs() {
        int keepDays = luminaProperties.getStats().getLogKeepDays();
        if (keepDays <= 0) {
            log.info("日志保留天数配置为 {}, 跳过日志清理", keepDays);
            return;
        }

        log.info("开始执行过期日志清理任务，保留天数: {}", keepDays);

        try {
            // 计算过期时间的时间戳（秒），需与 RequestLog 中的 requestTime 单位一致
            long expireTimestamp = Instant.now().minus(keepDays, ChronoUnit.DAYS).getEpochSecond();
            
            int deletedCount = requestLogService.deleteLogsOlderThan(expireTimestamp);
            
            log.info("过期日志清理任务完成，共清理了 {} 条过期日志", deletedCount);
        } catch (Exception e) {
            log.error("执行过期日志清理任务时发生异常", e);
        }
    }
}
