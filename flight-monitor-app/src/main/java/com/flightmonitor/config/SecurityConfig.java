package com.flightmonitor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * Spring Security configuration.
 * <p>
 * REST API endpoints are stateless (no session, no CSRF token required).
 * Web UI endpoints enable CSRF and full session management.
 * Sensitive actuator endpoints require authentication.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Public safe actuator endpoints only
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info"
                        ).permitAll()
                        // All other actuator endpoints require authentication
                        .requestMatchers("/actuator/**").authenticated()
                        // H2 console restricted to authenticated users
                        .requestMatchers("/h2-console/**").authenticated()
                        // Public static resources and UI
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/api/v1/**",
                                "/flights/**",
                                "/alerts/**",
                                "/reports/**",
                                "/",
                                "/error",
                                "/static/**",
                                "/css/**",
                                "/js/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Disable CSRF only for stateless REST API paths; web UI keeps CSRF protection
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/api/v1/**"),
                                new AntPathRequestMatcher("/h2-console/**")
                        )
                )
                .headers(headers -> headers
                        // Allow H2 console frames from same origin (dev only)
                        .frameOptions(frameOptions -> frameOptions.sameOrigin())
                        // Prevent MIME-type sniffing (nosniff)
                        .contentTypeOptions(Customizer.withDefaults())
                        // Enable XSS filter in legacy browsers
                        .xssProtection(Customizer.withDefaults())
                        // Strict referrer policy
                        .referrerPolicy(referrer ->
                                referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        // Content Security Policy
                        // NOTE: 'unsafe-inline' for script-src/style-src is temporary for Thymeleaf
                        // template compatibility. Future improvement: migrate to nonce-based CSP.
                        .contentSecurityPolicy(csp ->
                                csp.policyDirectives(
                                        "default-src 'self'; " +
                                        "script-src 'self' 'unsafe-inline'; " +
                                        "style-src 'self' 'unsafe-inline'; " +
                                        "img-src 'self' data:; " +
                                        "frame-ancestors 'self'"))
                );

        return http.build();
    }
}
