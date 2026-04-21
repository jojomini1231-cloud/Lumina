package com.lumina;

import com.lumina.config.LuminaProperties;
import com.lumina.scheduled.LogCleanupJob;
import com.lumina.service.RequestLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

public class LogCleanupJobTest {

    @Mock
    private RequestLogService requestLogService;

    @Mock
    private LuminaProperties luminaProperties;

    @Mock
    private LuminaProperties.Stats stats;

    @InjectMocks
    private LogCleanupJob logCleanupJob;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(luminaProperties.getStats()).thenReturn(stats);
    }

    @Test
    void testCleanupUsesSeconds() {
        // Arrange
        int keepDays = 7;
        when(stats.getLogKeepDays()).thenReturn(keepDays);
        when(requestLogService.deleteLogsOlderThan(anyLong())).thenReturn(10);

        // Act
        logCleanupJob.cleanupExpiredLogs();

        // Assert
        ArgumentCaptor<Long> timestampCaptor = ArgumentCaptor.forClass(Long.class);
        verify(requestLogService).deleteLogsOlderThan(timestampCaptor.capture());

        long capturedTimestamp = timestampCaptor.getValue();
        
        // 预期的过期时间（秒级）
        long expectedTimestampSeconds = Instant.now().minus(keepDays, ChronoUnit.DAYS).getEpochSecond();
        
        // 允许有一点时间差 (2秒内)
        assertTrue(Math.abs(expectedTimestampSeconds - capturedTimestamp) <= 2, 
            "时间戳应该是秒级，预期约为: " + expectedTimestampSeconds + ", 实际为: " + capturedTimestamp);
            
        // 如果是毫秒级，会比秒级大1000倍左右
        assertTrue(capturedTimestamp < Instant.now().toEpochMilli() / 100,
            "时间戳看起来像是毫秒级，这会导致错误删除近期日志");
    }
}
