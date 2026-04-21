package in.respondlyai.gateway.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Spring Boot will automatically inject the list from application.yml here!
    @Value("${application.security.public-paths}")
    private List<String> publicPaths;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isSecured(request)) {
            if (request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION) == null) {
                return onError(exchange, "Authorization header is missing", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization header format", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            if (jwtUtil.isInvalid(token)) {
                return onError(exchange, "Authorization token is invalid or expired", HttpStatus.UNAUTHORIZED);
            }

            try {
                var claims = jwtUtil.getAllClaimsFromToken(token);
                String userId = claims.get("userId", String.class);
                String role = claims.get("role", String.class);
                String orgId = claims.get("orgId", String.class);

                ServerHttpRequest.Builder requestBuilder = exchange.getRequest()
                        .mutate()
                        .header("X-User-Id", userId)
                        .header("X-User-Role", role)
                        .headers(headers -> headers.remove(HttpHeaders.AUTHORIZATION));

                if (orgId != null && !orgId.isEmpty()) {
                    requestBuilder.header("X-Org-Id", orgId);
                }

                request = requestBuilder.build();

            } catch (Exception e) {
                return onError(exchange, "Failed to parse JWT claims", HttpStatus.UNAUTHORIZED);
            }
        }

        return chain.filter(exchange.mutate().request(request).build());
    }

    private boolean isSecured(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        // If the current path starts with or contains any of our public paths, it is NOT secured.
        return publicPaths.stream().noneMatch(path::contains);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String err, HttpStatus httpStatus) {
        log.warn("Security Block: {}", err);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -1;
    }
}