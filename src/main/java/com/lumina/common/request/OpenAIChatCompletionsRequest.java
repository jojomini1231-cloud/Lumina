package com.lumina.common.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lumina.dto.ModelGroupConfigItem;
import com.lumina.logging.LogWriter;
import com.lumina.logging.RequestLogContext;
import com.lumina.util.SnowflakeIdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
public class OpenAIChatCompletionsRequest {

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Autowired
    private LogWriter logWriter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Map<String, String> URI_MAP;

    static {
        URI_MAP = Map.of(
                "openai_chat_completions", "/chat/completions",
                "openai_responses", "/responses",
                "anthropic_messages", "/messages"
        );
    }

    public Flux<ServerSentEvent<String>> streamChat(
            ObjectNode request,
            ModelGroupConfigItem provider,
            Boolean beta,
            String type
    ) {
        RequestLogContext ctx = new RequestLogContext();
        ctx.setProviderId(provider.getProviderId());
        ctx.setProviderName(provider.getProviderName());
        ctx.setId(snowflakeIdGenerator.nextId());
        ctx.setRequestId(UUID.randomUUID().toString());
        ctx.setStartNano(System.nanoTime());
        ctx.setRequestTime(System.currentTimeMillis() / 1000);
        ctx.setRequestType(type);
        ctx.setStream(true);
        ctx.setRequestModel(request.path("model").asText());
        ctx.setRequestContent(request.toPrettyString());

        WebClient webClient = WebClient.builder()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                .build();

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(URI_MAP.get(type))
                        .queryParam("beta", beta)
                        .build()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .doOnNext(event -> {
                    String data = event.data();
                    if (data == null) return;

                    if (ctx.getFirstTokenArrived().compareAndSet(false, true)) {
                        ctx.setFirstTokenMs((int)((System.nanoTime() - ctx.getStartNano()) / 1_000_000));
                    }

                    if ("[DONE]".equals(data)) return;

                    ctx.getResponseBuffer().append(data);

                    try {
                        JsonNode chunk = objectMapper.readTree(data);
                        if (chunk.has("usage")) {
                            JsonNode usage = chunk.get("usage");
                            if (usage.has("prompt_tokens")) ctx.setInputTokens(usage.get("prompt_tokens").asInt());
                            if (usage.has("completion_tokens")) ctx.setOutputTokens(usage.get("completion_tokens").asInt());
                        }
                    } catch (Exception e) {
                        log.debug("解析token失败: {}", e.getMessage());
                    }
                })
                .doOnError(err -> {
                    ctx.setStatus("FAIL");
                    ctx.setErrorStage("HTTP");
                    ctx.setErrorMessage(err.getMessage());
                    ctx.setTotalTimeMs((int)((System.nanoTime() - ctx.getStartNano()) / 1_000_000));
                    logWriter.submit(ctx);
                })
                .doOnComplete(() -> {
                    ctx.setTotalTimeMs((int)((System.nanoTime() - ctx.getStartNano()) / 1_000_000));
                    ctx.setResponseContent(ctx.getResponseBuffer().toString());
                    logWriter.submit(ctx);
                });
    }

    public Mono<ObjectNode> normalChat(
            ObjectNode request,
            ModelGroupConfigItem provider,
            Boolean beta,
            String type
    ) {
        RequestLogContext ctx = new RequestLogContext();
        ctx.setId(snowflakeIdGenerator.nextId());
        ctx.setProviderId(provider.getProviderId());
        ctx.setProviderName(provider.getProviderName());
        ctx.setRequestId(UUID.randomUUID().toString());
        ctx.setStartNano(System.nanoTime());
        ctx.setRequestTime(System.currentTimeMillis() / 1000);
        ctx.setRequestType(type);
        ctx.setStream(false);
        ctx.setRequestModel(request.path("model").asText());
        ctx.setRequestContent(request.toPrettyString());

        WebClient webClient = WebClient.builder()
                .baseUrl(provider.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + provider.getApiKey())
                .build();

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(URI_MAP.get(type))
                        .queryParam("beta", beta)
                        .build()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ObjectNode.class)
                .doOnNext(resp -> {
                    ctx.setTotalTimeMs((int)((System.nanoTime() - ctx.getStartNano()) / 1_000_000));
                    ctx.setResponseContent(resp.toString());

                    if (resp.has("usage")) {
                        JsonNode usage = resp.get("usage");
                        if (usage.has("prompt_tokens")) ctx.setInputTokens(usage.get("prompt_tokens").asInt());
                        if (usage.has("completion_tokens")) ctx.setOutputTokens(usage.get("completion_tokens").asInt());
                    }

                    logWriter.submit(ctx);
                })
                .doOnError(err -> {
                    ctx.setStatus("FAIL");
                    ctx.setErrorStage("HTTP");
                    ctx.setErrorMessage(err.getMessage());
                    ctx.setTotalTimeMs((int)((System.nanoTime() - ctx.getStartNano()) / 1_000_000));
                    logWriter.submit(ctx);
                });
    }
}