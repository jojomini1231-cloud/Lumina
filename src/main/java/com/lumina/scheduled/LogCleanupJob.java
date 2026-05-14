package com.lumina.scheduled;

import com.lumina.config.LuminaProperties;
import com.lumina.service.RequestLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogCleanupJob {

    private final RequestLogService requestLogService;
    private final LuminaProperties luminaProperties;

    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredLogs() {
        cleanupContent();
        cleanupLogs();
    }

    private void cleanupContent() {
        int contentKeepDays = luminaProperties.getStats().getContentKeepDays();
        if (contentKeepDays <= 0) {
            return;
        }

        log.info("开始清理过期请求/响应内容，保留天数: {}", contentKeepDays);
        try {
            long expireTimestamp = Instant.now().minus(contentKeepDays, ChronoUnit.DAYS).getEpochSecond();
            int clearedCount = requestLogService.clearContentOlderThan(expireTimestamp);
            log.info("过期内容清理完成，共清理了 {} 条记录的请求/响应内容", clearedCount);
        } catch (Exception e) {
            log.error("清理过期请求/响应内容时发生异常", e);
        }
    }

    private void cleanupLogs() {
        int keepDays = luminaProperties.getStats().getLogKeepDays();
        if (keepDays <= 0) {
            return;
        }

        log.info("开始清理过期日志，保留天数: {}", keepDays);
        try {
            long expireTimestamp = Instant.now().minus(keepDays, ChronoUnit.DAYS).getEpochSecond();
            int deletedCount = requestLogService.deleteLogsOlderThan(expireTimestamp);
            log.info("过期日志清理完成，共清理了 {} 条记录", deletedCount);
        } catch (Exception e) {
            log.error("清理过期日志时发生异常", e);
        }
    }
}
