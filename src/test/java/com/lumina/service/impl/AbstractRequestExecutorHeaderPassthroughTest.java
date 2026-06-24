package com.lumina.service.impl;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumina.dto.ModelGroupConfigItem;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AbstractRequestExecutorHeaderPassthroughTest {

    private final TestRequestExecutor executor = new TestRequestExecutor();

    @Test
    void passesApplicationHeadersAndAnthropicBeta() {
        HttpHeaders inbound = new HttpHeaders();
        inbound.add("anthropic-beta", "context-1m-2025-08-07");
        inbound.add("X-Trace-Id", "trace-1");
        inbound.add("Host", "lumina.local");
        inbound.add("Content-Length", "123");
        inbound.add(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate, br");
        inbound.add(HttpHeaders.CONTENT_ENCODING, "gzip");
        inbound.add(HttpHeaders.AUTHORIZATION, "Bearer lumina-client-key");
        inbound.add("x-api-key", "lumina-client-key");

        ModelGroupConfigItem provider = new ModelGroupConfigItem();
        provider.setProviderType(2);
        provider.setApiKey("provider-key");

        HttpHeaders target = new HttpHeaders();
        executor.apply(target, inbound, provider);

        assertEquals("context-1m-2025-08-07", target.getFirst("anthropic-beta"));
        assertEquals("trace-1", target.getFirst("X-Trace-Id"));
        assertEquals("provider-key", target.getFirst("x-api-key"));
        assertFalse(target.containsKey("Host"));
        assertFalse(target.containsKey("Content-Length"));
        assertFalse(target.containsKey(HttpHeaders.ACCEPT_ENCODING));
        assertFalse(target.containsKey(HttpHeaders.CONTENT_ENCODING));
        assertFalse(target.containsKey(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void usesProviderAuthorizationForOpenAiCompatibleRequests() {
        HttpHeaders inbound = new HttpHeaders();
        inbound.add(HttpHeaders.AUTHORIZATION, "Bearer lumina-client-key");
        inbound.add("x-api-key", "lumina-client-key");
        inbound.add("X-Request-Id", "request-1");

        ModelGroupConfigItem provider = new ModelGroupConfigItem();
        provider.setProviderType(0);
        provider.setApiKey("provider-key");

        HttpHeaders target = new HttpHeaders();
        executor.apply(target, inbound, provider);

        assertEquals("Bearer provider-key", target.getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals("request-1", target.getFirst("X-Request-Id"));
        assertFalse(target.containsKey("x-api-key"));
    }

    @Test
    void addsAnthropicOneMillionContextBetaWhenProviderBetaIsEnabled() {
        ModelGroupConfigItem provider = new ModelGroupConfigItem();
        provider.setProviderType(2);
        provider.setBeta(true);
        provider.setApiKey("provider-key");

        HttpHeaders target = new HttpHeaders();
        executor.apply(target, new HttpHeaders(), provider);

        assertEquals("context-1m-2025-08-07", target.getFirst("anthropic-beta"));
    }

    @Test
    void doesNotDuplicateAnthropicOneMillionContextBeta() {
        HttpHeaders inbound = new HttpHeaders();
        inbound.add("anthropic-beta", "context-1m-2025-08-07");

        ModelGroupConfigItem provider = new ModelGroupConfigItem();
        provider.setProviderType(2);
        provider.setBeta(true);
        provider.setApiKey("provider-key");

        HttpHeaders target = new HttpHeaders();
        executor.apply(target, inbound, provider);

        assertEquals("context-1m-2025-08-07", target.getFirst("anthropic-beta"));
    }

    @Test
    void keepsAnthropicBetaUnsetWhenProviderBetaIsDisabled() {
        ModelGroupConfigItem provider = new ModelGroupConfigItem();
        provider.setProviderType(2);
        provider.setBeta(false);
        provider.setApiKey("provider-key");

        HttpHeaders target = new HttpHeaders();
        executor.apply(target, new HttpHeaders(), provider);

        assertFalse(target.containsKey("anthropic-beta"));
    }

    private static class TestRequestExecutor extends AbstractRequestExecutor {
        void apply(HttpHeaders target, HttpHeaders source, ModelGroupConfigItem provider) {
            applyPassthroughHeaders(target, source, provider);
        }

        @Override
        public boolean supports(String type) {
            return true;
        }

        @Override
        public Mono<ObjectNode> executeNormal(ObjectNode request, ModelGroupConfigItem provider,
                                              Map<String, String> queryParams, HttpHeaders requestHeaders,
                                              String modelAction, String type, Integer timeoutMs) {
            return Mono.empty();
        }

        @Override
        public Flux<ServerSentEvent<String>> executeStream(ObjectNode request, ModelGroupConfigItem provider,
                                                           Map<String, String> queryParams, HttpHeaders requestHeaders,
                                                           String modelAction, String type, Integer timeoutMs) {
            return Flux.empty();
        }
    }
}
