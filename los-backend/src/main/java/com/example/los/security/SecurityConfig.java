package com.example.los.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"success\":false,\"message\":\"Chưa đăng nhập hoặc token không hợp lệ (Unauthorized)\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Static resources & SPA
                .requestMatchers("/", "/index.html", "/assets/**", "/favicon.ico", "/*.ico", "/*.png", "/*.svg", "/*.js", "/*.css").permitAll()
                // Public APIs (Không cần token)
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/otp/**").permitAll()
                // Protected APIs (Phải có token hợp lệ)
                .requestMatchers("/api/v1/**").authenticated()
                // Các route còn lại (SPA navigation)
                .anyRequest().permitAll()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
    }
}
