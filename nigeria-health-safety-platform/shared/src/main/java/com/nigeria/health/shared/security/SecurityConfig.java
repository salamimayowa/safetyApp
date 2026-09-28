package com.nigeria.health.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * KEY DECISIONS:
 * - Stateless (no sessions) — JWT handles authentication
 * - CSRF disabled — safe for stateless REST APIs
 * - @EnableMethodSecurity → allows @PreAuthorize on controller methods
 * - BCrypt strength 12 — strong enough for production, not too slow
 *
 * PUBLIC ENDPOINTS (no JWT required):
 * - All /api/v1/auth/** routes
 * - GET /api/v1/hospitals (browse list)
 * - GET /api/v1/drugs/verify/** (any citizen can verify a drug)
 * - POST /api/v1/accidents/report (citizen can report without account)
 * - Swagger UI endpoints
 *
 * Everything else requires a valid JWT.
 * Fine-grained role control is done with @PreAuthorize in controllers.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ── Auth endpoints — always public ──
                .requestMatchers("/api/v1/auth/**").permitAll()

                // ── Blood Bank — public browse ──
                .requestMatchers(HttpMethod.GET, "/api/v1/hospitals").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/hospitals/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/blood-stock/search").permitAll()

                // ── Accident Report — citizen can report without account ──
                .requestMatchers(HttpMethod.POST, "/api/v1/accidents/report").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accidents/{id}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/accidents/{id}/updates").permitAll()

                // ── Drug Verification — any citizen can verify ──
                .requestMatchers(HttpMethod.GET, "/api/v1/drugs/verify/**").permitAll()

                // ── Swagger / OpenAPI ──
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**"
                ).permitAll()

                // ── Everything else requires authentication ──
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * BCrypt strength 12 — hashes passwords securely.
     * Never store plain text passwords anywhere.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
