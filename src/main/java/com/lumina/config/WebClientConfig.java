package com.lumina.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.net.InetSocketAddress;
import java.net.Proxy;
import reactor.netty.transport.ProxyProvider;

@Configuration
public class WebClientConfig {

    @Bean
    public ConnectionProvider relayConnectionProvider(LuminaProperties properties) {
        LuminaProperties.Relay relay = properties.getRelay();
        return ConnectionProvider.builder("lumina-relay")
                .maxConnections(relay.getMaxConnections())
                .pendingAcquireMaxCount(relay.getPendingAcquireMaxCount())
                .pendingAcquireTimeout(Duration.ofMillis(relay.getPendingAcquireTimeoutMs()))
                .maxIdleTime(Duration.ofSeconds(relay.getMaxIdleTimeSeconds()))
                .maxLifeTime(Duration.ofSeconds(relay.getMaxLifeTimeSeconds()))
                .evictInBackground(Duration.ofSeconds(Math.max(30, relay.getMaxIdleTimeSeconds())))
                .metrics(true)
                .build();
    }

    @Bean
    public HttpClient relayHttpClient(ConnectionProvider relayConnectionProvider, LuminaProperties properties) {
        LuminaProperties.Relay relay = properties.getRelay();
        HttpClient httpClient = HttpClient.create(relayConnectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, relay.getConnectTimeoutMs())
                .responseTimeout(Duration.ofMillis(relay.getResponseTimeoutMs()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(relay.getResponseTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(relay.getResponseTimeoutMs(), java.util.concurrent.TimeUnit.MILLISECONDS)));
                        
        if (properties.getProxy().isEnabled() && properties.getProxy().getUrl() != null && !properties.getProxy().getUrl().isEmpty()) {
            try {
                String proxyUrl = properties.getProxy().getUrl();
                java.net.URI uri = new java.net.URI(proxyUrl);
                httpClient = httpClient.proxy(proxy -> proxy
                        .type(uri.getScheme().startsWith("socks") ? ProxyProvider.Proxy.SOCKS5 : ProxyProvider.Proxy.HTTP)
                        .host(uri.getHost())
                        .port(uri.getPort() > 0 ? uri.getPort() : (uri.getScheme().startsWith("socks") ? 1080 : 80))
                        .connectTimeoutMillis(properties.getProxy().getTimeout()));
            } catch (Exception e) {
                // Log exception but continue without proxy
            }
        }
        
        return httpClient;
    }

    @Bean
    public WebClient.Builder webClientBuilder(HttpClient relayHttpClient, LuminaProperties properties) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs()
                        .maxInMemorySize(properties.getRelay().getMaxInMemorySizeMb() * 1024 * 1024))
                .build();

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(relayHttpClient))
                .exchangeStrategies(strategies);
    }
}
