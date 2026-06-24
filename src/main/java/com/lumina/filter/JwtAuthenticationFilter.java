package com.lumina.filter;

import com.lumina.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(0)
public class JwtAuthenticationFilter implements WebFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ReactiveUserDetailsService userDetailsService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // 跳过 /v1/** 和 /v1beta/** 路径，这些路径使用 ApiKey 认证
        if (path.startsWith("/v1/") || path.startsWith("/v1beta/")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        String jwt = jwtUtil.getTokenFromHeader(authHeader);

        if (StringUtils.hasText(jwt) && !hasJwtShape(jwt)) {
            log.warn("Rejected malformed JWT for request: method={}, path={}, remoteAddress={}, authHeader={}",
                    exchange.getRequest().getMethod(),
                    getPathWithQuery(exchange),
                    exchange.getRequest().getRemoteAddress(),
                    describeAuthorizationHeader(authHeader));
            return chain.filter(exchange);
        }

        if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
            String username = jwtUtil.getUsernameFromToken(jwt);

            return userDetailsService.findByUsername(username)
                    .flatMap(userDetails -> {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        log.debug("Set reactive authentication for user: {}", username);

                        // 在 WebFlux 中，安全上下文存储在 Reactor Context 中
                        return chain.filter(exchange)
                                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication));
                    })
                    .onErrorResume(e -> {
                        log.error("Could not set user authentication in reactive security context", e);
                        return chain.filter(exchange);
                    });
        }

        if (StringUtils.hasText(jwt)) {
            log.warn("JWT validation failed for request: method={}, path={}, remoteAddress={}, authHeader={}",
                    exchange.getRequest().getMethod(),
                    getPathWithQuery(exchange),
                    exchange.getRequest().getRemoteAddress(),
                    describeAuthorizationHeader(authHeader));
        }

        return chain.filter(exchange);
    }

    private boolean hasJwtShape(String token) {
        return countPeriods(token) == 2;
    }

    private String getPathWithQuery(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String query = exchange.getRequest().getURI().getRawQuery();
        return StringUtils.hasText(query) ? path + "?" + query : path;
    }

    private String describeAuthorizationHeader(String authHeader) {
        if (!StringUtils.hasText(authHeader)) {
            return "missing";
        }
        if (!authHeader.startsWith("Bearer ")) {
            return "presentWithoutBearer length=" + authHeader.length();
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        return "Bearer tokenLength=" + token.length() + ", tokenPeriods=" + countPeriods(token);
    }

    private int countPeriods(String token) {
        int count = 0;
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) == '.') {
                count++;
            }
        }
        return count;
    }
}
