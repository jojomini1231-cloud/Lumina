package com.lumina.aspect;

import com.lumina.service.ApiKeyConcurrencyLimiter;
import com.lumina.service.ApiKeyService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
public class ApiKeyConcurrencyLimitAspect {

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private ApiKeyConcurrencyLimiter concurrencyLimiter;

    @Around("execution(public reactor.core.publisher.Mono com.lumina.controller.RelayController.create*(..))")
    public Object limitApiKeyConcurrency(ProceedingJoinPoint joinPoint) {
        ServerWebExchange exchange = findExchange(joinPoint.getArgs());
        String apiKey = exchange != null ? exchange.getAttribute("API_KEY") : null;

        if (!StringUtils.hasText(apiKey)) {
            return proceed(joinPoint);
        }

        return apiKeyService.getMaxConcurrentRequests(apiKey)
                .flatMap(limit -> {
                    if (limit == null || limit <= 0) {
                        return toMono(proceed(joinPoint));
                    }

                    ApiKeyConcurrencyLimiter.Permit permit = concurrencyLimiter.tryAcquire(apiKey, limit);
                    try {
                        return toMono(proceed(joinPoint))
                                .map(response -> wrapResponseIfStreaming(response, permit))
                                .doOnError(error -> permit.release())
                                .doOnCancel(permit::release)
                                .doOnSuccess(response -> {
                                    if (!isStreamingResponse(response)) {
                                        permit.release();
                                    }
                                });
                    } catch (Throwable error) {
                        permit.release();
                        return Mono.error(error);
                    }
                });
    }

    private Object proceed(ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (Throwable error) {
            return Mono.error(error);
        }
    }

    @SuppressWarnings("unchecked")
    private Mono<ResponseEntity<?>> toMono(Object value) {
        if (value instanceof Mono<?> mono) {
            return (Mono<ResponseEntity<?>>) mono;
        }
        return Mono.error(new IllegalStateException("Relay controller must return Mono<ResponseEntity<?>>"));
    }

    private ResponseEntity<?> wrapResponseIfStreaming(ResponseEntity<?> response, ApiKeyConcurrencyLimiter.Permit permit) {
        Object body = response.getBody();
        if (!(body instanceof Publisher<?> publisher)) {
            return response;
        }

        Flux<?> wrappedBody = Flux.from(publisher).doFinally(signalType -> permit.release());
        return ResponseEntity.status(response.getStatusCode())
                .headers(headers -> headers.addAll(response.getHeaders()))
                .body(wrappedBody);
    }

    private boolean isStreamingResponse(ResponseEntity<?> response) {
        return response != null && response.getBody() instanceof Publisher<?>;
    }

    private ServerWebExchange findExchange(Object[] args) {
        if (args == null) {
            return null;
        }
        for (Object arg : args) {
            if (arg instanceof ServerWebExchange exchange) {
                return exchange;
            }
        }
        return null;
    }
}
