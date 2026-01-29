package com.lumina.state;

import java.util.concurrent.atomic.LongAdder;

/**
 * 高性能时间窗口指标收集器（环形桶实现）
 *
 * 优点：
 * - 更新 O(1)，无对象分配
 * - 避免 stream() 遍历
 * - 线程安全，使用 LongAdder 实现高并发
 */
public class SlidingWindowMetrics {

    private final int bucketCount;
    private final long bucketDurationMs;
    private final Bucket[] buckets;
    private volatile int currentIndex;

    /**
     * 单个时间桶
     */
    static class Bucket {
        final LongAdder total = new LongAdder();
        final LongAdder errors = new LongAdder();
        final LongAdder slow = new LongAdder();
        volatile long epochMs;  // 桶的起始时间，用于懒清理

        void reset(long newEpochMs) {
            total.reset();
            errors.reset();
            slow.reset();
            epochMs = newEpochMs;
        }

        long getTotal() {
            return total.sum();
        }

        long getErrors() {
            return errors.sum();
        }

        long getSlow() {
            return slow.sum();
        }
    }

    /**
     * 构造函数
     * @param bucketCount 桶数量（例如 10）
     * @param bucketDurationMs 每个桶的时间跨度（例如 1000ms）
     */
    public SlidingWindowMetrics(int bucketCount, long bucketDurationMs) {
        this.bucketCount = bucketCount;
        this.bucketDurationMs = bucketDurationMs;
        this.buckets = new Bucket[bucketCount];

        long now = System.currentTimeMillis();
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new Bucket();
            buckets[i].epochMs = now - (bucketCount - 1 - i) * bucketDurationMs;
        }
        this.currentIndex = bucketCount - 1;
    }

    /**
     * 默认配置：10 个桶，每桶 1 秒，总窗口 10 秒
     */
    public SlidingWindowMetrics() {
        this(10, 1000);
    }

    /**
     * 记录一次请求结果
     * @param success 是否成功
     * @param isSlow 是否为慢调用
     */
    public void record(boolean success, boolean isSlow) {
        Bucket bucket = getCurrentBucket();
        bucket.total.increment();
        if (!success) {
            bucket.errors.increment();
        }
        if (isSlow) {
            bucket.slow.increment();
        }
    }

    /**
     * 获取当前桶（懒清理过期桶）
     */
    private Bucket getCurrentBucket() {
        long now = System.currentTimeMillis();
        long currentBucketStart = (now / bucketDurationMs) * bucketDurationMs;

        Bucket bucket = buckets[currentIndex];

        // 检查当前桶是否过期
        if (bucket.epochMs < currentBucketStart) {
            // 需要推进到新桶
            synchronized (this) {
                // Double-check
                bucket = buckets[currentIndex];
                if (bucket.epochMs < currentBucketStart) {
                    // 计算需要推进多少个桶
                    long bucketsToAdvance = (currentBucketStart - bucket.epochMs) / bucketDurationMs;

                    if (bucketsToAdvance >= bucketCount) {
                        // 全部过期，重置所有桶
                        for (int i = 0; i < bucketCount; i++) {
                            buckets[i].reset(currentBucketStart - (bucketCount - 1 - i) * bucketDurationMs);
                        }
                        currentIndex = bucketCount - 1;
                    } else {
                        // 部分推进
                        for (int i = 0; i < bucketsToAdvance; i++) {
                            currentIndex = (currentIndex + 1) % bucketCount;
                            buckets[currentIndex].reset(currentBucketStart - (bucketsToAdvance - 1 - i) * bucketDurationMs);
                        }
                    }
                    bucket = buckets[currentIndex];
                }
            }
        }

        return bucket;
    }

    /**
     * 获取窗口内的总请求数
     */
    public long getTotalCount() {
        refreshWindow();
        long total = 0;
        long cutoff = System.currentTimeMillis() - bucketCount * bucketDurationMs;
        for (Bucket bucket : buckets) {
            if (bucket.epochMs >= cutoff) {
                total += bucket.getTotal();
            }
        }
        return total;
    }

    /**
     * 获取窗口内的错误数
     */
    public long getErrorCount() {
        refreshWindow();
        long errors = 0;
        long cutoff = System.currentTimeMillis() - bucketCount * bucketDurationMs;
        for (Bucket bucket : buckets) {
            if (bucket.epochMs >= cutoff) {
                errors += bucket.getErrors();
            }
        }
        return errors;
    }

    /**
     * 获取窗口内的慢调用数
     */
    public long getSlowCount() {
        refreshWindow();
        long slow = 0;
        long cutoff = System.currentTimeMillis() - bucketCount * bucketDurationMs;
        for (Bucket bucket : buckets) {
            if (bucket.epochMs >= cutoff) {
                slow += bucket.getSlow();
            }
        }
        return slow;
    }

    /**
     * 获取错误率
     * @return 错误率 (0.0 - 1.0)，如果无请求返回 0.0
     */
    public double getErrorRate() {
        long total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) getErrorCount() / total;
    }

    /**
     * 获取慢调用率
     * @return 慢调用率 (0.0 - 1.0)，如果无请求返回 0.0
     */
    public double getSlowRate() {
        long total = getTotalCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) getSlowCount() / total;
    }

    /**
     * 刷新窗口（触发懒清理）
     */
    private void refreshWindow() {
        getCurrentBucket();
    }

    /**
     * 重置所有指标
     */
    public void reset() {
        long now = System.currentTimeMillis();
        synchronized (this) {
            for (int i = 0; i < bucketCount; i++) {
                buckets[i].reset(now - (bucketCount - 1 - i) * bucketDurationMs);
            }
            currentIndex = bucketCount - 1;
        }
    }

    /**
     * 获取窗口总时长（毫秒）
     */
    public long getWindowDurationMs() {
        return bucketCount * bucketDurationMs;
    }
}
