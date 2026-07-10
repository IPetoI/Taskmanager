package com.ipetoi.taskmanager.config;

import com.ipetoi.taskmanager.security.JwtAuthenticationFilter;
import com.ipetoi.taskmanager.security.JwtTokenProvider;
import com.ipetoi.taskmanager.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configures the HTTP security filter chain: endpoints are either public or
 * require authentication through {@link JwtAuthenticationFilter}.
 * The PasswordEncoder bean is defined in {@link ApplicationConfig}.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider tokenProvider, UserService userService) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // API endpoints that require authentication/authorization.
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/tasks/**").hasRole("USER")
                        // Swagger (dev/test): public access.
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // Everything else (static files, index.html, CSS, JS): public.
                        .anyRequest().permitAll()
                );

        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(tokenProvider, userService);
        http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}