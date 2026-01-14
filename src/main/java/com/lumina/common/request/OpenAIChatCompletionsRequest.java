package com.lumina.common.request;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class OpenAIChatCompletionsRequest {

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
            String apiKey,
            String baseUrl,
            Boolean beta,
            String type
    ) {

        System.out.println("请求体: " + request.toPrettyString());

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        long startNano = System.nanoTime();
        AtomicBoolean firstToken = new AtomicBoolean(true);
        StringBuilder responseBuffer = new StringBuilder();

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(URI_MAP.get(type))
                        // ⚠️ 是否保留 beta 你后面可以按 provider 控制
                        .queryParam("beta", beta)
                        .build()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                // ✅ 关键修复点：明确 SSE + String
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {
                })
                // ===== 旁路观测，不破坏流 =====
                .doOnNext(event -> {
                    String data = event.data();
                    if (data == null) {
                        return;
                    }

                    // 首 token 时间
                    if (firstToken.compareAndSet(true, false)) {
                        long firstMs = (System.nanoTime() - startNano) / 1_000_000;
                        System.out.println("首 token 时间(ms): " + firstMs);
                    }

                    // DONE 是协议控制符，不是 JSON
                    if ("[DONE]".equals(data)) {
                        System.out.println("收到 DONE 信号");
                        return;
                    }

                    // 普通 chunk
                    responseBuffer.append(data);
                    System.out.println("收到数据 chunk: " + data);
                })
                .doOnError(err -> {
                    System.err.println("请求出错: " + err.getMessage());
                })
                .doOnComplete(() -> {
                    long totalMs = (System.nanoTime() - startNano) / 1_000_000;
                    System.out.println("流式响应结束，总耗时(ms): " + totalMs);
                    System.out.println("完整响应内容: " + responseBuffer);
                    // 👉 这里可以异步写 request_logs
                });
    }

    public Mono<ObjectNode> normalChat(
            ObjectNode request,
            String apiKey,
            String baseUrl,
            Boolean beta,
            String type
    ) {

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path(URI_MAP.get(type))
                        // ⚠️ 是否保留 beta 你后面可以按 provider 控制
                        .queryParam("beta", beta)
                        .build()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ObjectNode.class)
                .doOnNext(resp -> {
                    System.out.println("非流式完整响应: " + resp);
                });
    }
}