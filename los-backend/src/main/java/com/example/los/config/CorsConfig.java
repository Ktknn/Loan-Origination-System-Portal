package com.example.los.config;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.annotation.PostConstruct;

@Configuration
public class CorsConfig {

    private static final Logger log = LoggerFactory.getLogger(CorsConfig.class);

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @PostConstruct
    public void logCorsConfig() {
        log.info(">>> CORS Allowed Origins: [{}]", allowedOrigins);
    }

    /**
     * FilterRegistrationBean explicitly overrides CorsFilter's own LOWEST_PRECEDENCE
     * order (from GenericFilterBean). Without this wrapper, @Order on @Bean is ignored
     * because Spring Boot uses the filter's own getOrder() method.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration() {
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        log.info(">>> CORS FilterRegistrationBean origins: {}", origins);

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Type"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        // HIGHEST_PRECEDENCE ensures this runs before Spring Security's DelegatingFilterProxy
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
