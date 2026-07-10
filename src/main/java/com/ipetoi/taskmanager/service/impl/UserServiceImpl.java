package com.ipetoi.taskmanager.service.impl;

import com.ipetoi.taskmanager.exception.DuplicateResourceException;
import com.ipetoi.taskmanager.exception.InvalidCredentialsException;
import com.ipetoi.taskmanager.exception.ResourceNotFoundException;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.model.UserRole;
import com.ipetoi.taskmanager.repository.UserRepository;
import com.ipetoi.taskmanager.service.UserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createUser(String username, String password, String email) {
        return createUser(username, password, email, UserRole.USER);
    }

    @Override
    public User createUser(String username, String password, String email, UserRole role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new DuplicateResourceException("Username already exists: " + username);
        }
        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateResourceException("Email already exists: " + email);
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setEmail(email);
        u.setRole(role == null ? UserRole.USER : role);

        return userRepository.save(u);
    }

    @Override
    public User authenticate(String username, String password) {
        User u = userRepository.findByUsername(username)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

        if (!passwordEncoder.matches(password, u.getPassword())) {
            throw new InvalidCredentialsException("Invalid username or password");
        }
        return u;
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public List<User> findAllNonAdminUsers() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() != UserRole.ADMIN)
                .collect(Collectors.toList());
    }

    @Override
    public User ensureAdminAccount(String username, String rawPassword, String email) {
        User existing = userRepository.findByUsername(username).orElse(null);
        if (existing == null) {
            return createUser(username, rawPassword, email, UserRole.ADMIN);
        }

        boolean changed = false;
        if (existing.getRole() != UserRole.ADMIN) {
            existing.setRole(UserRole.ADMIN);
            changed = true;
        }
        if (email != null && !email.equals(existing.getEmail())) {
            existing.setEmail(email);
            changed = true;
        }
        if (rawPassword != null && !passwordEncoder.matches(rawPassword, existing.getPassword())) {
            existing.setPassword(passwordEncoder.encode(rawPassword));
            changed = true;
        }
        return changed ? userRepository.save(existing) : existing;
    }

    @Override
    public User updateUser(Long id, String username, String email, String rawPassword) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (u.getRole() == UserRole.ADMIN) {
            throw new AccessDeniedException("Admin account cannot be edited here");
        }

        if (username != null && !username.isBlank() && !username.equals(u.getUsername())) {
            userRepository.findByUsername(username).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new DuplicateResourceException("Username already exists: " + username);
                }
            });
            u.setUsername(username);
        }

        if (email != null && !email.isBlank() && !email.equals(u.getEmail())) {
            userRepository.findByEmail(email).ifPresent(other -> {
                if (!other.getId().equals(id)) {
                    throw new DuplicateResourceException("Email already exists: " + email);
                }
            });
            u.setEmail(email);
        }

        if (rawPassword != null && !rawPassword.isBlank()) {
            u.setPassword(passwordEncoder.encode(rawPassword));
        }

        return userRepository.save(u);
    }

    @Override
    public void deleteUser(Long id) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));

        if (u.getRole() == UserRole.ADMIN) {
            throw new AccessDeniedException("Admin account cannot be deleted");
        }

        userRepository.delete(u);
    }
}