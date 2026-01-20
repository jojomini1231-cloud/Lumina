package com.lumina.state;

import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class ProviderScoreCalculator {

    private static final double ALPHA = 0.4;
    private static final double LATENCY_SAFE_THRESHOLD_MS = 5000.0;
    private static final double LATENCY_MAX_THRESHOLD_MS = 30000.0;
    private static final int WINDOW_SIZE = 20;
    private static final int MIN_REQUESTS_FOR_SCORE = 15;  // 最少请求数

    public void update(ProviderRuntimeState stats, boolean success, long latencyMs) {

        updateBasicStats(stats, success, latencyMs);

        // HALF_OPEN 特殊处理
        if (stats.getCircuitState() == CircuitState.HALF_OPEN) {
            handleHalfOpenScore(stats, success);
            return;
        }

        recalcScore(stats);
    }

    private void handleHalfOpenScore(ProviderRuntimeState stats, boolean success) {
        if (success) {
            // 试探成功，给予恢复机会
            stats.setScore(60.0);
            stats.setSuccessRateEma(0.8);
            // 不清空窗口，让它自然恢复
        } else {
            // 试探失败，保持低分
            stats.setScore(5.0);
        }
    }

    private void updateBasicStats(ProviderRuntimeState stats, boolean success, long latencyMs) {
        stats.getTotalRequests().incrementAndGet();

        if (success) {
            stats.getSuccessRequests().incrementAndGet();
        } else {
            stats.getFailureRequests().incrementAndGet();
        }

        updateSlidingWindow(stats, success);

        // 延迟 EMA
        double oldLatency = stats.getLatencyEmaMs();
        stats.setLatencyEmaMs(
                oldLatency == 0 ? latencyMs : ALPHA * latencyMs + (1 - ALPHA) * oldLatency
        );

        // 成功率 EMA
        double currentSuccess = success ? 1.0 : 0.0;
        double oldSuccessRate = stats.getSuccessRateEma();
        stats.setSuccessRateEma(
                oldSuccessRate == 0 && stats.getTotalRequests().get() == 1
                        ? currentSuccess
                        : ALPHA * currentSuccess + (1 - ALPHA) * oldSuccessRate
        );
    }

    private void updateSlidingWindow(ProviderRuntimeState stats, boolean success) {
        Queue<Boolean> window = stats.getRecentResults();
        window.offer(success);
        if (window.size() > WINDOW_SIZE) {
            window.poll();
        }
    }

    private void recalcScore(ProviderRuntimeState stats) {

        long total = stats.getTotalRequests().get();

        // 请求数不足时的策略
        if (total < MIN_REQUESTS_FOR_SCORE) {
            long failures = stats.getFailureRequests().get();
            if (failures == 0) {
                stats.setScore(80.0);
            } else if (failures >= total) {
                stats.setScore(20.0);  // 全失败也不给0分
            } else {
                stats.setScore(50.0);  // 有成功有失败
            }
            return;
        }

        // 1. 延迟惩罚
        double currentLatency = stats.getLatencyEmaMs();
        double latencyPenalty = 0;

        if (currentLatency > LATENCY_SAFE_THRESHOLD_MS) {
            latencyPenalty = Math.min(
                    (currentLatency - LATENCY_SAFE_THRESHOLD_MS) /
                            (LATENCY_MAX_THRESHOLD_MS - LATENCY_SAFE_THRESHOLD_MS),
                    1.0
            );
        }

        // 2. 从滑动窗口计算最近失败率
        Queue<Boolean> window = stats.getRecentResults();
        long recentFailures = window.stream().filter(r -> !r).count();
        double recentFailureRate = window.isEmpty() ? 0 : recentFailures * 1.0 / window.size();

        // 3. 最终评分
        double score =
                stats.getSuccessRateEma() * 70
                        - latencyPenalty * 20
                        - recentFailureRate * 10;

        stats.setScore(Math.max(1.0, Math.min(100, score)));
    }
}
