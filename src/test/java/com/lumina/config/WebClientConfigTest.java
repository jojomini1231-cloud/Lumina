package com.lumina.config;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.netty.resources.ConnectionProvider;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebClientConfigTest {

    @Test
    void relayWebClientDecodesGzipJsonResponses() throws Exception {
        byte[] responseBody = gzip("{\"ok\":true}");
        DisposableServer server = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .route(routes -> routes.get("/gzip", (request, response) -> response
                        .header(HttpHeaderNames.CONTENT_ENCODING, "gzip")
                        .header(HttpHeaderNames.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .sendByteArray(reactor.core.publisher.Mono.just(responseBody))))
                .bindNow();

        WebClientConfig config = new WebClientConfig();
        LuminaProperties properties = new LuminaProperties();
        ConnectionProvider connectionProvider = config.relayConnectionProvider(properties);

        try {
            WebClient webClient = config.webClientBuilder(
                            config.relayHttpClient(connectionProvider, properties),
                            properties)
                    .baseUrl("http://127.0.0.1:" + server.port())
                    .build();

            ObjectNode body = webClient.get()
                    .uri("/gzip")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(ObjectNode.class)
                    .block(Duration.ofSeconds(5));

            assertTrue(body.path("ok").asBoolean());
        } finally {
            server.disposeNow();
            connectionProvider.disposeLater().block(Duration.ofSeconds(5));
        }
    }

    private static byte[] gzip(String value) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
            gzip.write(value.getBytes(StandardCharsets.UTF_8));
        }
        return output.toByteArray();
    }
}
