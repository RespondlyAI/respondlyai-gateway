package in.respondlyai.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayRoutingConfig {

    @Value("${AUTH_SERVICE_URL:http://auth-service.respondlyai.svc.cluster.local:8080}")
    private String authServiceUrl;

    @Value("${ORG_SERVICE_URL:http://org-service.respondlyai.svc.cluster.local:8080}")
    private String orgServiceUrl;

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/v1/auth/**")
                        .uri(authServiceUrl))

                .route("auth-docs", r -> r.path("/swagger-docs/auth")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(authServiceUrl))
                
                .route("org-service", r -> r.path("/api/v1/org/**")
                        .uri(orgServiceUrl))
                 
                .route("org-docs", r -> r.path("/swagger-docs/org")
                        .filters(f -> f.setPath("/v3/api-docs"))
                        .uri(orgServiceUrl))
                .build();
    }
}
