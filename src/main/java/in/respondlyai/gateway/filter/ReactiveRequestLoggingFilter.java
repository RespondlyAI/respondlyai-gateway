package in.respondlyai.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReactiveRequestLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();

        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getURI().getPath();
        String query = exchange.getRequest().getURI().getQuery();
        String queryString = query != null ? "?" + query : "";
        String remoteAddr = exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";

        log.info("--> {} {}{} from [{}]", method, path, queryString, remoteAddr);

        return chain.filter(exchange)
                .doOnSuccess(aVoid -> logResponse(exchange, startTime, method, path))
                .doOnError(throwable -> logError(exchange, startTime, method, path, throwable));
    }

    private void logResponse(ServerWebExchange exchange, long startTime, String method, String path) {
        long duration = System.currentTimeMillis() - startTime;
        int status = exchange.getResponse().getStatusCode() != null ?
                exchange.getResponse().getStatusCode().value() : 200;

        if (status >= 500) {
            log.error("<-- {} {} {} ({}ms)", method, path, status, duration);
        } else if (status >= 400) {
            log.warn("<-- {} {} {} ({}ms)", method, path, status, duration);
        } else {
            log.info("<-- {} {} {} ({}ms)", method, path, status, duration);
        }
    }

    private void logError(ServerWebExchange exchange, long startTime, String method, String path, Throwable throwable) {
        long duration = System.currentTimeMillis() - startTime;
        log.error("<-- {} {} 500 ({}ms) - Exception: {}", method, path, duration, throwable.getMessage());
    }
}