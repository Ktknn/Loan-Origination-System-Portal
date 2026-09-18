package com.example.los.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CorsConfig — cho phép frontend (Vercel) gọi backend (Render) cross-origin.
 * Allowed origins được inject từ env var FRONTEND_URL.
 *
 * Exposes a CorsConfigurationSource @Bean so that Spring Security's
 * Customizer.withDefaults() can auto-discover it and apply CORS headers
 * before the security filter chain blocks the request.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_URL}")
    private String allowedOrigins;

    /**
     * Used by Spring Security (SecurityConfig → cors(Customizer.withDefaults())).
     * Without this bean, Spring Security would not add any CORS headers.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Trim each origin in case of accidental leading/trailing spaces in env
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList();
        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    /** Also configure MVC-layer CORS (e.g. for non-Security-filtered paths). */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                        Arrays.stream(allowedOrigins.split(","))
                              .map(String::trim)
                              .toArray(String[]::new)
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
