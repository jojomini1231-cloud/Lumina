package com.lumina.service;

import com.lumina.dto.DashboardOverviewDto;
import com.lumina.dto.ModelTokenUsageDto;
import com.lumina.dto.ProviderStatsDto;
import com.lumina.dto.RequestTrafficDto;
import com.lumina.mapper.DashboardMapper;
import com.lumina.mapper.ProviderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    @Autowired
    private ProviderMapper providerMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 获取仪表盘概览统计
     */
    public DashboardOverviewDto getOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last24Hours = now.minusHours(24);
        LocalDateTime previous24Hours = now.minusHours(48);

        DashboardOverviewDto current = dashboardMapper.getOverviewStats(last24Hours.format(FORMATTER));
        DashboardOverviewDto previous = dashboardMapper.getPreviousPeriodStats(
                previous24Hours.format(FORMATTER),
                last24Hours.format(FORMATTER)
        );

        if (current != null) {
            if (previous != null && previous.getTotalRequests() > 0) {
                current.setRequestGrowthRate(
                        ((current.getTotalRequests() - previous.getTotalRequests()) * 100.0) / previous.getTotalRequests()
                );
                current.setCostGrowthRate(
                        ((current.getTotalCost().doubleValue() - previous.getTotalCost().doubleValue()) * 100.0) / previous.getTotalCost().doubleValue()
                );
                current.setLatencyChange(current.getAvgLatency() - previous.getAvgLatency());
                current.setSuccessRateChange(current.getSuccessRate() - previous.getSuccessRate());
            } else {
                current.setRequestGrowthRate(0.0);
                current.setCostGrowthRate(0.0);
                current.setLatencyChange(0.0);
                current.setSuccessRateChange(0.0);
            }
        }

        return current;
    }

    /**
     * 获取24小时请求流量
     */
    public List<RequestTrafficDto> getRequestTraffic() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        return dashboardMapper.getRequestTraffic(last24Hours.format(FORMATTER));
    }

    /**
     * 获取模型 Token 使用统计
     */
    public List<ModelTokenUsageDto> getModelTokenUsage() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<ModelTokenUsageDto> usageList = dashboardMapper.getModelTokenUsage(last24Hours.format(FORMATTER));

        long totalTokens = usageList.stream()
                .mapToLong(ModelTokenUsageDto::getTotalTokens)
                .sum();

        usageList.forEach(usage -> {
            if (totalTokens > 0) {
                usage.setPercentage((usage.getTotalTokens() * 100.0) / totalTokens);
            } else {
                usage.setPercentage(0.0);
            }
        });

        return usageList;
    }

    /**
     * 获取供应商统计排名
     */
    public List<ProviderStatsDto> getProviderStats(Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 10;
        }

        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        List<ProviderStatsDto> statsList = dashboardMapper.getProviderStats(last24Hours.format(FORMATTER), limit);

        for (int i = 0; i < statsList.size(); i++) {
            ProviderStatsDto stats = statsList.get(i);
            stats.setRank(i + 1);
        }

        return statsList;
    }
}
