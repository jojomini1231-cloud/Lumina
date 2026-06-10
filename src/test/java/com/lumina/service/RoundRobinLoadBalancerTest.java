package com.lumina.service;

import com.lumina.dto.ModelGroupConfigItem;
import com.lumina.exception.NoHealthyProviderException;
import com.lumina.metrics.RelayMetrics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RoundRobinLoadBalancerTest {

    @Mock
    private RelayMetrics relayMetrics;

    @Test
    void selectsProvidersInRoundRobinOrderPerGroup() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        List<ModelGroupConfigItem> items = List.of(provider("provider-a"), provider("provider-b"));

        assertEquals("provider-a", loadBalancer.select(items, Set.of(), "group-a").getProviderName());
        assertEquals("provider-b", loadBalancer.select(items, Set.of(), "group-a").getProviderName());
        assertEquals("provider-a", loadBalancer.select(items, Set.of(), "group-a").getProviderName());

        verify(relayMetrics, times(3)).recordSelection("round_robin");
    }

    @Test
    void excludesAlreadyTriedProviders() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        ModelGroupConfigItem providerA = provider("provider-a");
        ModelGroupConfigItem providerB = provider("provider-b");

        ModelGroupConfigItem selected = loadBalancer.select(
                List.of(providerA, providerB),
                Set.of(providerId(providerA)),
                "group-b"
        );

        assertEquals("provider-b", selected.getProviderName());
    }

    @Test
    void roundRobinPositionAdvancesOnFilteredCandidates() {
        // Verifies the counter advances per call, not per eligible item.
        // After filtering, index falls on the surviving sub-list uniformly.
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        ModelGroupConfigItem providerA = provider("provider-a");
        ModelGroupConfigItem providerB = provider("provider-b");
        ModelGroupConfigItem providerC = provider("provider-c");
        List<ModelGroupConfigItem> items = List.of(providerA, providerB, providerC);

        // counter=0 → filter to [A,B,C] → index 0 → A
        assertEquals("provider-a", loadBalancer.select(items, Set.of(), "group-stable").getProviderName());

        // counter=1 → filter to [B,C] (A excluded) → index 1%2=1 → C
        ModelGroupConfigItem selected = loadBalancer.select(
                items,
                Set.of(providerId(providerA)),
                "group-stable"
        );

        assertEquals("provider-c", selected.getProviderName());
    }

    @Test
    void usesFloorModWhenCounterOverflows() throws Exception {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        setCounter(loadBalancer, "group-overflow", Integer.MIN_VALUE);
        List<ModelGroupConfigItem> items = List.of(provider("provider-a"), provider("provider-b"), provider("provider-c"));

        ModelGroupConfigItem selected = loadBalancer.select(items, Set.of(), "group-overflow");

        assertEquals("provider-b", selected.getProviderName());
    }

    @Test
    void throwsSpecificErrorWhenGroupHasNoProviders() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                loadBalancer.select(List.of(), Set.of(), "group-empty"));

        assertEquals("模型组未配置 Provider，轮询无可用候选", error.getMessage());
    }

    @Test
    void throwsWhenAllProvidersAreExcluded() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        ModelGroupConfigItem provider = provider("provider-a");

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                loadBalancer.select(List.of(provider), Set.of(providerId(provider)), "group-c"));

        assertEquals("所有 Provider 已尝试过，轮询无可用候选", error.getMessage());
    }

    @Test
    void throwsWhenAllProvidersFailAvailabilityPredicate() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        ModelGroupConfigItem provider = provider("provider-a");

        RuntimeException error = assertThrows(RuntimeException.class, () ->
                loadBalancer.select(List.of(provider), Set.of(), "group-unavailable", item -> false));

        assertEquals("所有 Provider 当前不可用，轮询无可用候选", error.getMessage());
    }

    @Test
    void usesDefaultCounterWhenGroupIdIsNull() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        List<ModelGroupConfigItem> items = List.of(provider("provider-a"), provider("provider-b"));

        assertEquals("provider-a", loadBalancer.select(items, Set.of(), null).getProviderName());
        assertEquals("provider-b", loadBalancer.select(items, Set.of(), null).getProviderName());
        assertEquals("provider-a", loadBalancer.select(items, Set.of(), null).getProviderName());
    }

    @Test
    void throwsNoHealthyProviderExceptionWhenAllFailPredicate() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        ModelGroupConfigItem provider = provider("provider-a");

        assertInstanceOf(NoHealthyProviderException.class,
                assertThrows(RuntimeException.class, () ->
                        loadBalancer.select(List.of(provider), Set.of(), "group-unhealthy", item -> false)));
    }

    @Test
    void distributesEvenlyWithPersistentlyExcludedProvider() {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        ModelGroupConfigItem a = provider("provider-a");
        ModelGroupConfigItem b = provider("provider-b");
        ModelGroupConfigItem c = provider("provider-c");
        List<ModelGroupConfigItem> items = List.of(a, b, c);

        int countB = 0;
        int countC = 0;
        for (int i = 0; i < 300; i++) {
            ModelGroupConfigItem selected = loadBalancer.select(items, Set.of(providerId(a)), "group-skew");
            if (selected.getProviderName().equals("provider-b")) countB++;
            if (selected.getProviderName().equals("provider-c")) countC++;
        }

        // With A excluded, B and C should each get ~150 selections (within ±20 tolerance for 300 rounds)
        assertTrue(countB >= 130, "B should get roughly half: " + countB);
        assertTrue(countC >= 130, "C should get roughly half: " + countC);
        assertEquals(300, countB + countC);
    }

    @Test
    void distributesEvenlyUnderConcurrentAccess() throws Exception {
        RoundRobinLoadBalancer loadBalancer = new RoundRobinLoadBalancer(relayMetrics);
        List<ModelGroupConfigItem> items = List.of(
                provider("provider-a"), provider("provider-b"), provider("provider-c"));
        int threads = 6;
        int perThread = 300;
        ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < perThread; i++) {
                    String name = loadBalancer.select(items, Set.of(), "group-concurrent").getProviderName();
                    counts.computeIfAbsent(name, k -> new AtomicInteger()).incrementAndGet();
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        pool.shutdownNow();

        int expected = threads * perThread / items.size();
        int tolerance = expected / 10; // 10% tolerance for scheduling jitter
        assertEquals(expected, counts.get("provider-a").get(), tolerance);
        assertEquals(expected, counts.get("provider-b").get(), tolerance);
        assertEquals(expected, counts.get("provider-c").get(), tolerance);
    }

    private ModelGroupConfigItem provider(String name) {
        ModelGroupConfigItem item = new ModelGroupConfigItem();
        item.setProviderName(name);
        item.setModelName("gpt-test");
        item.setBaseUrl("https://" + name + ".example.com");
        item.setApiKey(name + "-key");
        return item;
    }

    private String providerId(ModelGroupConfigItem item) {
        return com.lumina.util.ProviderIdGenerator.generate(item);
    }

    @SuppressWarnings("unchecked")
    private void setCounter(RoundRobinLoadBalancer loadBalancer, String groupId, int value) throws Exception {
        Field countersField = RoundRobinLoadBalancer.class.getDeclaredField("counters");
        countersField.setAccessible(true);
        Map<String, AtomicInteger> counters =
                (ConcurrentHashMap<String, AtomicInteger>) countersField.get(loadBalancer);
        counters.put(groupId, new AtomicInteger(value));
    }
}
