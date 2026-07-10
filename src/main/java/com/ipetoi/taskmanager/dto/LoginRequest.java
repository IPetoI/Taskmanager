package com.ipetoi.taskmanager.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login request DTO (POST /api/auth/login). Contains only the fields required for
 * authentication - separate from UserDto, which also includes the email field.
 */
public class LoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}