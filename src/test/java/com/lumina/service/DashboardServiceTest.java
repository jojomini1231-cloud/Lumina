package com.lumina.service;

import com.lumina.dto.HealthHeatmapDto;
import com.lumina.dto.RequestTrafficDto;
import com.lumina.mapper.DashboardMapper;
import com.lumina.mapper.StatsDailyMapper;
import com.lumina.mapper.StatsHourlyMapper;
import com.lumina.stats.StatsRedisReader;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private DashboardMapper dashboardMapper;

    @Mock
    private StatsDailyMapper statsDailyMapper;

    @Mock
    private StatsHourlyMapper statsHourlyMapper;

    @Mock
    private StatsRedisReader statsRedisReader;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService();
        ReflectionTestUtils.setField(dashboardService, "dashboardMapper", dashboardMapper);
        ReflectionTestUtils.setField(dashboardService, "statsDailyMapper", statsDailyMapper);
        ReflectionTestUtils.setField(dashboardService, "statsHourlyMapper", statsHourlyMapper);
        ReflectionTestUtils.setField(dashboardService, "statsRedisReader", statsRedisReader);
        ReflectionTestUtils.setField(dashboardService, "meterRegistry", new SimpleMeterRegistry());
        ReflectionTestUtils.setField(dashboardService, "statsTimeZone", "Asia/Shanghai");
        ReflectionTestUtils.setField(dashboardService, "clock",
                Clock.fixed(Instant.parse("2026-06-07T02:37:42Z"), ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void requestTrafficUsesConfiguredTimezoneCurrentHourAndEpochBuckets() {
        ReflectionTestUtils.setField(dashboardService, "clock",
                Clock.fixed(Instant.parse("2026-06-07T09:37:42Z"), ZoneId.of("Asia/Shanghai")));

        when(dashboardMapper.getRequestTrafficByRequestTime(1780740000L, 1780826400L, 28800))
                .thenReturn(List.of(
                        RequestTrafficDto.builder()
                                .hour(18)
                                .requestCount(11L)
                                .timestamp(1780740000000L)
                                .build(),
                        RequestTrafficDto.builder()
                                .hour(17)
                                .requestCount(42L)
                                .timestamp(1780822800000L)
                                .build()
                ));

        List<RequestTrafficDto> traffic = dashboardService.getRequestTraffic();

        verify(dashboardMapper).getRequestTrafficByRequestTime(1780740000L, 1780826400L, 28800);
        assertEquals(24, traffic.size());

        RequestTrafficDto firstBucket = traffic.get(0);
        assertEquals(18, firstBucket.getHour());
        assertEquals(1780740000000L, firstBucket.getTimestamp());
        assertEquals(11L, firstBucket.getRequestCount());

        RequestTrafficDto lastBucket = traffic.get(23);
        assertEquals(17, lastBucket.getHour());
        assertEquals(1780822800000L, lastBucket.getTimestamp());
        assertEquals(42L, lastBucket.getRequestCount());
    }

    @Test
    void requestTrafficDoesNotMergeSameClockHourAcrossDays() {
        ReflectionTestUtils.setField(dashboardService, "clock",
                Clock.fixed(Instant.parse("2026-06-07T10:37:42Z"), ZoneId.of("Asia/Shanghai")));

        when(dashboardMapper.getRequestTrafficByRequestTime(anyLong(), anyLong(), anyInt()))
                .thenReturn(List.of(
                        RequestTrafficDto.builder()
                                .hour(18)
                                .requestCount(7L)
                                .timestamp(1780740000000L)
                                .build(),
                        RequestTrafficDto.builder()
                                .hour(18)
                                .requestCount(19L)
                                .timestamp(1780826400000L)
                                .build()
                ));

        List<RequestTrafficDto> traffic = dashboardService.getRequestTraffic();

        assertEquals(19L, traffic.get(23).getRequestCount());
        assertTrue(traffic.stream().noneMatch(bucket -> bucket.getRequestCount() == 7L));
        assertEquals(19L, traffic.stream()
                .filter(bucket -> bucket.getTimestamp().equals(1780826400000L))
                .findFirst()
                .map(RequestTrafficDto::getRequestCount)
                .orElse(0L));
    }

    @Test
    void healthHeatmapUsesRealQuarterHourBuckets() {
        when(dashboardMapper.getHealthHeatmapBuckets(1780713900L, 1780800300L))
                .thenReturn(List.of(
                        HealthHeatmapDto.HeatmapBucket.builder()
                                .bucketStartEpoch(1780797600L)
                                .totalRequests(28L)
                                .successRequests(25L)
                                .build(),
                        HealthHeatmapDto.HeatmapBucket.builder()
                                .bucketStartEpoch(1780798500L)
                                .totalRequests(338L)
                                .successRequests(332L)
                                .build()
                ));

        HealthHeatmapDto heatmap = dashboardService.getHealthHeatmap(1);

        verify(dashboardMapper).getHealthHeatmapBuckets(1780713900L, 1780800300L);
        assertEquals(96, heatmap.getCells().size());

        HealthHeatmapDto.HeatmapCell firstBucket = heatmap.getCells().get(0);
        assertEquals(1780713900000L, firstBucket.getTimestamp());

        HealthHeatmapDto.HeatmapCell tenOClock = heatmap.getCells().get(93);
        assertEquals(1780797600000L, tenOClock.getTimestamp());
        assertEquals(28, tenOClock.getTotalRequests());
        assertEquals(25, tenOClock.getSuccessRequests());
        assertEquals(25 * 100.0 / 28, tenOClock.getSuccessRate(), 0.000001);

        HealthHeatmapDto.HeatmapCell tenFifteen = heatmap.getCells().get(94);
        assertEquals(1780798500000L, tenFifteen.getTimestamp());
        assertEquals(338, tenFifteen.getTotalRequests());
        assertEquals(332, tenFifteen.getSuccessRequests());
        assertEquals(332 * 100.0 / 338, tenFifteen.getSuccessRate(), 0.000001);

        HealthHeatmapDto.HeatmapCell currentBucket = heatmap.getCells().get(95);
        assertEquals(1780799400000L, currentBucket.getTimestamp());
        assertEquals(0, currentBucket.getTotalRequests());
        assertEquals(0, currentBucket.getSuccessRequests());
        assertEquals(-1, currentBucket.getSuccessRate());

        HealthHeatmapDto.HeatmapCell emptyBucket = heatmap.getCells().get(92);
        assertEquals(0, emptyBucket.getTotalRequests());
        assertEquals(0, emptyBucket.getSuccessRequests());
        assertEquals(-1, emptyBucket.getSuccessRate());

        assertEquals((25 + 332) * 100.0 / (28 + 338), heatmap.getOverallSuccessRate(), 0.000001);
    }
}
