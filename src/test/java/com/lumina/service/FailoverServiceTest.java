package com.lumina.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumina.config.CircuitBreakerConfig;
import com.lumina.config.CircuitBreakerConfigResolver;
import com.lumina.config.EffectiveCircuitBreakerConfig;
import com.lumina.config.LuminaProperties;
import com.lumina.dto.ModelGroupConfig;
import com.lumina.dto.ModelGroupConfigItem;
import com.lumina.mapper.ProviderRuntimeStatsMapper;
import com.lumina.metrics.RelayMetrics;
import com.lumina.state.CircuitBreaker;
import com.lumina.state.CircuitBreakerEventLogger;
import com.lumina.state.CircuitState;
import com.lumina.state.FailureType;
import com.lumina.state.ProviderScoreCalculator;
import com.lumina.state.ProviderRuntimeState;
import com.lumina.state.ProviderStateRegistry;
import com.lumina.util.ProviderIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FailoverServiceTest {

    private static final int ROUND_ROBIN_MODE = 1;

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private ProviderScoreCalculator scoreCalculator;

    @Mock
    private CircuitBreaker circuitBreaker;

    @Mock
    private RelayMetrics relayMetrics;

    @Mock
    private ProviderRuntimeStatsMapper providerRuntimeStatsMapper;

    private FailoverService failoverService;

    @BeforeEach
    void setUp() {
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        ProviderStateRegistry providerStateRegistry = new ProviderStateRegistry(providerRuntimeStatsMapper, circuitBreakerConfig);
        CircuitBreakerConfigResolver configResolver = new CircuitBreakerConfigResolver(circuitBreakerConfig);

        failoverService = new FailoverService(
                providerStateRegistry,
                scoreCalculator,
                circuitBreaker,
                configResolver,
                relayMetrics,
                new LuminaProperties(),
                new RoundRobinLoadBalancer(relayMetrics)
        );
    }

    @Test
    void roundRobinSuccessDoesNotUpdateScoreOrCircuitBreaker() {
        ObjectNode response = mapper.createObjectNode().put("ok", true);

        ObjectNode actual = failoverService.executeWithFailoverMono(
                provider -> Mono.just(response),
                roundRobinGroup("rr-success"),
                1000
        ).block(Duration.ofSeconds(1));

        assertEquals(response, actual);
        verifyNoInteractions(scoreCalculator, circuitBreaker);
    }

    @Test
    void roundRobinFailoverDoesNotUpdateScoreOrCircuitBreaker() {
        AtomicInteger calls = new AtomicInteger();
        List<String> providerNames = new ArrayList<>();

        ObjectNode actual = failoverService.executeWithFailoverMono(
                provider -> {
                    providerNames.add(provider.getProviderName());
                    if (calls.getAndIncrement() == 0) {
                        return Mono.error(new RuntimeException("first provider failed"));
                    }
                    return Mono.just(mapper.createObjectNode().put("provider", provider.getProviderName()));
                },
                roundRobinGroup("rr-failover"),
                1000
        ).block(Duration.ofSeconds(1));

        assertEquals("provider-b", actual.get("provider").asText());
        assertEquals(List.of("provider-a", "provider-b"), providerNames);
        verifyNoInteractions(scoreCalculator, circuitBreaker);
    }

    @Test
    void healthyRoundRobinSkipsProvidersRejectedByCircuitBreaker() {
        LuminaProperties properties = new LuminaProperties();
        properties.getFailover().setRoundRobinMode(LuminaProperties.RoundRobinMode.HEALTHY);
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        ProviderStateRegistry providerStateRegistry = new ProviderStateRegistry(providerRuntimeStatsMapper, circuitBreakerConfig);
        CircuitBreakerConfigResolver configResolver = new CircuitBreakerConfigResolver(circuitBreakerConfig);
        FailoverService healthyFailoverService = new FailoverService(
                providerStateRegistry,
                scoreCalculator,
                circuitBreaker,
                configResolver,
                relayMetrics,
                properties,
                new RoundRobinLoadBalancer(relayMetrics)
        );
        when(circuitBreaker.allowRequest(any(), any(EffectiveCircuitBreakerConfig.class)))
                .thenReturn(false, true);

        ModelGroupConfigItem selected = healthyFailoverService.selectAvailableProvider(roundRobinGroup("healthy-rr"));

        assertEquals("provider-b", selected.getProviderName());
        verify(circuitBreaker, times(2)).allowRequest(any(), any(EffectiveCircuitBreakerConfig.class));
        verify(relayMetrics).recordProviderSkipped(org.mockito.ArgumentMatchers.startsWith("round_robin_circuit_"));
        verify(relayMetrics).recordSelection("round_robin");
    }

    @Test
    void healthyRoundRobinRecordsFailureOnError() {
        LuminaProperties properties = new LuminaProperties();
        properties.getFailover().setRoundRobinMode(LuminaProperties.RoundRobinMode.HEALTHY);
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        ProviderStateRegistry providerStateRegistry = new ProviderStateRegistry(providerRuntimeStatsMapper, circuitBreakerConfig);
        CircuitBreakerConfigResolver configResolver = new CircuitBreakerConfigResolver(circuitBreakerConfig);
        FailoverService healthyFailoverService = new FailoverService(
                providerStateRegistry,
                scoreCalculator,
                circuitBreaker,
                configResolver,
                relayMetrics,
                properties,
                new RoundRobinLoadBalancer(relayMetrics)
        );
        when(circuitBreaker.allowRequest(any(), any(EffectiveCircuitBreakerConfig.class))).thenReturn(true);

        healthyFailoverService.executeWithFailoverMono(
                provider -> Mono.error(new RuntimeException("provider down")),
                roundRobinGroup("healthy-rr-failure"),
                1000
        ).onErrorResume(e -> Mono.empty()).block(Duration.ofSeconds(1));

        verify(circuitBreaker, org.mockito.Mockito.atLeastOnce())
                .onFailure(any(), any(FailureType.class), any(EffectiveCircuitBreakerConfig.class));
        verify(scoreCalculator, org.mockito.Mockito.atLeastOnce())
                .update(any(), any(FailureType.class), anyLong());
    }

    @Test
    void healthyRoundRobinFallsBackWhenAllProvidersCircuitOpen() {
        LuminaProperties properties = new LuminaProperties();
        properties.getFailover().setRoundRobinMode(LuminaProperties.RoundRobinMode.HEALTHY);
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        ProviderStateRegistry providerStateRegistry = new ProviderStateRegistry(providerRuntimeStatsMapper, circuitBreakerConfig);
        CircuitBreakerConfigResolver configResolver = new CircuitBreakerConfigResolver(circuitBreakerConfig);
        FailoverService healthyFailoverService = new FailoverService(
                providerStateRegistry,
                scoreCalculator,
                circuitBreaker,
                configResolver,
                relayMetrics,
                properties,
                new RoundRobinLoadBalancer(relayMetrics)
        );
        when(circuitBreaker.allowRequest(any(), any(EffectiveCircuitBreakerConfig.class))).thenReturn(false);

        ModelGroupConfigItem selected = healthyFailoverService.selectAvailableProvider(roundRobinGroup("healthy-rr-all-open"));

        assertEquals("provider-a", selected.getProviderName());
        verify(relayMetrics).recordFallbackToRoundRobin();
    }

    @Test
    void healthyRoundRobinUpdatesScoreAndCircuitBreakerOnSuccess() {
        LuminaProperties properties = new LuminaProperties();
        properties.getFailover().setRoundRobinMode(LuminaProperties.RoundRobinMode.HEALTHY);
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        ProviderStateRegistry providerStateRegistry = new ProviderStateRegistry(providerRuntimeStatsMapper, circuitBreakerConfig);
        CircuitBreakerConfigResolver configResolver = new CircuitBreakerConfigResolver(circuitBreakerConfig);
        FailoverService healthyFailoverService = new FailoverService(
                providerStateRegistry,
                scoreCalculator,
                circuitBreaker,
                configResolver,
                relayMetrics,
                properties,
                new RoundRobinLoadBalancer(relayMetrics)
        );
        when(circuitBreaker.allowRequest(any(), any(EffectiveCircuitBreakerConfig.class))).thenReturn(true);
        ObjectNode response = mapper.createObjectNode().put("ok", true);

        ObjectNode actual = healthyFailoverService.executeWithFailoverMono(
                provider -> Mono.just(response),
                roundRobinGroup("healthy-rr-success"),
                1000
        ).block(Duration.ofSeconds(1));

        assertEquals(response, actual);
        verify(scoreCalculator).update(any(), eq(FailureType.SUCCESS), anyLong());
        verify(circuitBreaker).onSuccess(any(), any(EffectiveCircuitBreakerConfig.class));
    }

    @Test
    void healthyRoundRobinDoesNotConsumeHalfOpenProbeForUnselectedProvider() {
        LuminaProperties properties = new LuminaProperties();
        properties.getFailover().setRoundRobinMode(LuminaProperties.RoundRobinMode.HEALTHY);
        CircuitBreakerConfig circuitBreakerConfig = new CircuitBreakerConfig();
        ProviderStateRegistry providerStateRegistry = new ProviderStateRegistry(providerRuntimeStatsMapper, circuitBreakerConfig);
        CircuitBreakerConfigResolver configResolver = new CircuitBreakerConfigResolver(circuitBreakerConfig);
        FailoverService healthyFailoverService = new FailoverService(
                providerStateRegistry,
                scoreCalculator,
                new CircuitBreaker(circuitBreakerConfig, new CircuitBreakerEventLogger(mapper)),
                configResolver,
                relayMetrics,
                properties,
                new RoundRobinLoadBalancer(relayMetrics)
        );
        ModelGroupConfig group = roundRobinGroup("healthy-rr-half-open-probe");
        ModelGroupConfigItem halfOpenProvider = group.getItems().get(1);
        ProviderRuntimeState halfOpenState = providerStateRegistry.get(ProviderIdGenerator.generate(halfOpenProvider));
        halfOpenState.setCircuitState(CircuitState.HALF_OPEN);
        halfOpenState.initHalfOpen(1);

        ModelGroupConfigItem selected = healthyFailoverService.selectAvailableProvider(group);

        assertEquals("provider-a", selected.getProviderName());
        assertEquals(1, halfOpenState.getProbeRemaining().get());
    }

    @Test
    void streamErrorBeforeFirstChunkFailsOverToNextProvider() {
        AtomicInteger calls = new AtomicInteger();
        List<String> providerNames = new ArrayList<>();

        List<ServerSentEvent<String>> events = failoverService.executeWithFailoverFlux(
                provider -> {
                    providerNames.add(provider.getProviderName());
                    if (calls.getAndIncrement() == 0) {
                        return Flux.error(new RuntimeException("first provider failed before first chunk"));
                    }
                    return Flux.just(ServerSentEvent.<String>builder()
                            .data("{\"provider\":\"" + provider.getProviderName() + "\"}")
                            .build());
                },
                roundRobinGroup("rr-stream-first-chunk-failover"),
                1000
        ).collectList().block(Duration.ofSeconds(1));

        assertEquals(List.of("provider-a", "provider-b"), providerNames);
        assertEquals(1, events.size());
        assertEquals("{\"provider\":\"provider-b\"}", events.get(0).data());
    }

    @Test
    void streamErrorAfterFirstChunkDoesNotFailOver() {
        List<String> providerNames = new ArrayList<>();

        Flux<ServerSentEvent<String>> result = failoverService.executeWithFailoverFlux(
                provider -> {
                    providerNames.add(provider.getProviderName());
                    return Flux.just(ServerSentEvent.<String>builder()
                                    .data("{\"content\":\"partial\"}")
                                    .build())
                            .concatWith(Flux.error(new RuntimeException("midstream failed")));
                },
                roundRobinGroup("rr-stream-midstream-failure"),
                1000
        );

        List<ServerSentEvent<String>> events = new ArrayList<>();
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        result.doOnNext(events::add)
                .doOnError(errorRef::set)
                .onErrorResume(error -> Mono.empty())
                .then()
                .block(Duration.ofSeconds(1));

        assertEquals(List.of("provider-a"), providerNames);
        assertEquals(2, events.size());
        assertEquals("{\"content\":\"partial\"}", events.get(0).data());
        org.junit.jupiter.api.Assertions.assertNotNull(errorRef.get());
        assertEquals("midstream failed", errorRef.get().getMessage());
        org.junit.jupiter.api.Assertions.assertTrue(events.get(1).data() != null
                && events.get(1).data().contains("网关传输中途发生网络异常中断"));
    }

    private ModelGroupConfig roundRobinGroup(String id) {
        ModelGroupConfig group = new ModelGroupConfig();
        group.setId(id);
        group.setName(id);
        group.setBalanceMode(ROUND_ROBIN_MODE);
        group.setItems(List.of(provider("provider-a"), provider("provider-b")));
        return group;
    }

    private ModelGroupConfigItem provider(String name) {
        ModelGroupConfigItem item = new ModelGroupConfigItem();
        item.setProviderName(name);
        item.setModelName("gpt-test");
        item.setBaseUrl("https://" + name + ".example.com");
        item.setApiKey(name + "-key");
        return item;
    }
}
