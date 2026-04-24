package com.riffly.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    // NOTE: Spring Security's filter chain processes requests before MVC,
    // so CORS is ALSO configured in SecurityConfig.corsConfigurationSource().
    // This WebMvcConfigurer entry handles non-security MVC paths and
    // ensures consistency for any requests that bypass the security filter.
    @Value("${riffly.cors.allowed-origins:http://localhost:5500,http://127.0.0.1:5500}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // "/**" — NOT "/api/**" — because context-path=/api is stripped before matching
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Range", "Accept-Ranges", "Content-Length", "Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
