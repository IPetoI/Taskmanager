package com.ipetoi.taskmanager.service;

import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.model.UserRole;

import java.util.List;

public interface UserService {

    User createUser(String username, String password, String email);

    User createUser(String username, String password, String email, UserRole role);

    User authenticate(String username, String password);

    User findByUsername(String username);

    // Returns only regular (non-admin) users - these are displayed in the admin panel.
    List<User> findAllNonAdminUsers();

    /**
     * Creates the admin account if it does not exist. If it already exists but its email,
     * password, or role differs from the configuration, updates those values.
     * This ensures there is always exactly one consistent admin account.
     */
    User ensureAdminAccount(String username, String rawPassword, String email);

    // Updates a regular user's data by an administrator. The admin account cannot be modified here.
    User updateUser(Long id, String username, String email, String rawPassword);

    // Deletes a regular user by an administrator (along with their tasks). The admin account cannot be deleted.
    void deleteUser(Long id);
}