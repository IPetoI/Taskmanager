package com.ipetoi.taskmanager.config;

import com.ipetoi.taskmanager.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Application-level beans that are not directly related to the Spring Security
 * filter chain configuration (see {@link SecurityConfig}).
 */
@Configuration
public class ApplicationConfig {

    /**
     * BCrypt password encoder: used by UserServiceImpl to hash and verify passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CommandLineRunner adminSeeder(UserService userService,
                                  @Value("${app.admin.username:admin}") String adminUsername,
                                  @Value("${app.admin.password:admin123}") String adminPassword,
                                  @Value("${app.admin.email:admin@example.com}") String adminEmail) {
        return args -> userService.ensureAdminAccount(adminUsername, adminPassword, adminEmail);
    }
}