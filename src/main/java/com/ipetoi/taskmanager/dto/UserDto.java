package com.ipetoi.taskmanager.dto;

import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.model.UserRole;

/**
 * Client-facing representation of the User entity (output only).
 * For registration input data, see {@link RegisterRequest}.
 */
public class UserDto {

    private Long id;
    private String username;
    private String email;
    private UserRole role;

    public UserDto() {
    }

    public UserDto(Long id, String username, String email, UserRole role) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
    }

    public static UserDto from(User user) {
        return new UserDto(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }
}