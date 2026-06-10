package com.lumina.service;

import com.lumina.dto.ModelGroupConfigItem;
import com.lumina.exception.NoHealthyProviderException;
import com.lumina.metrics.RelayMetrics;
import com.lumina.util.ProviderIdGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

@Component
@RequiredArgsConstructor
public class RoundRobinLoadBalancer {

    private final RelayMetrics relayMetrics;

    private final ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    /**
     * 轮询策略选择 Provider。
     */
    public ModelGroupConfigItem select(List<ModelGroupConfigItem> items, Set<String> excludeIds, String groupId) {
        return select(items, excludeIds, groupId, item -> true);
    }

    /**
     * 轮询策略选择 Provider，并允许调用方注入额外可用性判断。
     * 先过滤出存活候选再对其取模，保证流量在存活 Provider 间均匀分布；
     * 若改为先取模再向后跳过，被跳过者的后继会承接其全部流量份额，造成倾斜。
     * 可用性判断应为无副作用检查；需要在最终选中时执行预留逻辑的场景使用
     * {@link #selectAndReserve(List, Set, String, Predicate, Predicate)}。
     *
     * @throws NoHealthyProviderException 存在未尝试的 Provider 但全部未通过可用性判断，调用方可触发保底降级
     */
    public ModelGroupConfigItem select(
            List<ModelGroupConfigItem> items,
            Set<String> excludeIds,
            String groupId,
            Predicate<ModelGroupConfigItem> availabilityPredicate
    ) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("模型组未配置 Provider，轮询无可用候选");
        }

        List<ModelGroupConfigItem> notExcluded = items.stream()
                .filter(item -> {
                    boolean excluded = excludeIds.contains(ProviderIdGenerator.generate(item));
                    if (excluded) {
                        relayMetrics.recordProviderSkipped("round_robin_excluded");
                    }
                    return !excluded;
                })
                .toList();

        if (notExcluded.isEmpty()) {
            throw new RuntimeException("所有 Provider 已尝试过，轮询无可用候选");
        }

        List<ModelGroupConfigItem> candidates = notExcluded.stream()
                .filter(availabilityPredicate)
                .toList();

        if (candidates.isEmpty()) {
            throw new NoHealthyProviderException("所有 Provider 当前不可用，轮询无可用候选");
        }

        String key = groupId != null ? groupId : "default";
        AtomicInteger counter = counters.computeIfAbsent(key, k -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), candidates.size());

        relayMetrics.recordSelection("round_robin");
        return candidates.get(index);
    }

    /**
     * 轮询策略选择 Provider，并只对最终候选执行预留判断。
     * availabilityPredicate 用于无副作用地筛出候选集，reservationPredicate 可执行
     * HALF_OPEN 探测配额获取等有副作用逻辑。
     */
    public ModelGroupConfigItem selectAndReserve(
            List<ModelGroupConfigItem> items,
            Set<String> excludeIds,
            String groupId,
            Predicate<ModelGroupConfigItem> availabilityPredicate,
            Predicate<ModelGroupConfigItem> reservationPredicate
    ) {
        if (items == null || items.isEmpty()) {
            throw new RuntimeException("模型组未配置 Provider，轮询无可用候选");
        }

        List<ModelGroupConfigItem> notExcluded = items.stream()
                .filter(item -> {
                    boolean excluded = excludeIds.contains(ProviderIdGenerator.generate(item));
                    if (excluded) {
                        relayMetrics.recordProviderSkipped("round_robin_excluded");
                    }
                    return !excluded;
                })
                .toList();

        if (notExcluded.isEmpty()) {
            throw new RuntimeException("所有 Provider 已尝试过，轮询无可用候选");
        }

        List<ModelGroupConfigItem> candidates = notExcluded.stream()
                .filter(availabilityPredicate)
                .toList();

        if (candidates.isEmpty()) {
            throw new NoHealthyProviderException("所有 Provider 当前不可用，轮询无可用候选");
        }

        String key = groupId != null ? groupId : "default";
        AtomicInteger counter = counters.computeIfAbsent(key, k -> new AtomicInteger(0));
        synchronized (counter) {
            int start = Math.floorMod(counter.get(), candidates.size());
            for (int offset = 0; offset < candidates.size(); offset++) {
                ModelGroupConfigItem candidate = candidates.get((start + offset) % candidates.size());
                if (reservationPredicate.test(candidate)) {
                    counter.incrementAndGet();
                    relayMetrics.recordSelection("round_robin");
                    return candidate;
                }
            }
        }

        throw new NoHealthyProviderException("所有 Provider 当前不可用，轮询无可用候选");
    }
}
