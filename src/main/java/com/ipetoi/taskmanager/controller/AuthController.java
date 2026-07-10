package com.ipetoi.taskmanager.controller;

import com.ipetoi.taskmanager.dto.LoginRequest;
import com.ipetoi.taskmanager.dto.RegisterRequest;
import com.ipetoi.taskmanager.dto.UserDto;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.security.JwtTokenProvider;
import com.ipetoi.taskmanager.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Map;

/**
 * Authentication endpoints: user registration and login (returns a JWT on successful login).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtTokenProvider tokenProvider;

    public AuthController(UserService userService, JwtTokenProvider tokenProvider) {
        this.userService = userService;
        this.tokenProvider = tokenProvider;
    }

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequest req) {
        User created = userService.createUser(req.getUsername(), req.getPassword(), req.getEmail());
        return ResponseEntity.status(201).body(UserDto.from(created));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest dto) {
        User user = userService.authenticate(dto.getUsername(), dto.getPassword());
        String token = tokenProvider.createToken(user.getUsername());
        return ResponseEntity.ok(Map.of("token", token, "user", UserDto.from(user)));
    }
}