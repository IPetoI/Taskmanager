package com.ipetoi.taskmanager.repository;

import com.ipetoi.taskmanager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data generates the query from the method name: SELECT * FROM users WHERE username = ?.
    Optional<User> findByUsername(String username);

    // Finds a user by email address.
    Optional<User> findByEmail(String email);
}
