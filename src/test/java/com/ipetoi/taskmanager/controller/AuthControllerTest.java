package com.ipetoi.taskmanager.controller;

import com.ipetoi.taskmanager.dto.LoginRequest;
import com.ipetoi.taskmanager.config.SecurityConfig;
import com.ipetoi.taskmanager.exception.DuplicateResourceException;
import com.ipetoi.taskmanager.exception.InvalidCredentialsException;
import com.ipetoi.taskmanager.model.User;
import com.ipetoi.taskmanager.security.JwtTokenProvider;
import com.ipetoi.taskmanager.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    UserService userService;
    @MockBean
    JwtTokenProvider jwtTokenProvider;

    private User sampleUser;

    private String registerJson(String username, String password, String email) {
        ObjectNode body = objectMapper.createObjectNode();
        if (username != null) {
            body.put("username", username);
        }
        if (password != null) {
            body.put("password", password);
        }
        if (email != null) {
            body.put("email", email);
        }
        return body.toString();
    }

    @BeforeEach
    void setUp() {
        sampleUser = new User();
        sampleUser.setId(1L);
        sampleUser.setUsername("peto");
        sampleUser.setEmail("peto@example.com");
        sampleUser.setPassword("hashed");
    }

    // --- POST /api/auth/register ---

    @Test
    void registerShouldReturn201WithValidData() throws Exception {
        when(userService.createUser("peto", "jelszo123", "peto@example.com"))
                .thenReturn(sampleUser);

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("peto", "jelszo123", "peto@example.com")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("peto"))
                .andExpect(jsonPath("$.email").value("peto@example.com"))
                // Password is not included in UserDto, so it is never exposed in the response.
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerShouldReturn409ForDuplicateUsername() throws Exception {
        when(userService.createUser(any(), any(), any()))
                .thenThrow(new DuplicateResourceException("Username already exists: peto"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("peto", "jelszo123", "peto@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username already exists: peto"));
    }

    @Test
    void registerShouldReturn409ForDuplicateEmail() throws Exception {
        when(userService.createUser(any(), any(), any()))
                .thenThrow(new DuplicateResourceException("Email already exists: peto@example.com"));

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("peto", "jelszo123", "peto@example.com")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists: peto@example.com"));
    }

    @Test
    void registerShouldReturn400WhenUsernameIsMissing() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson(null, "jelszo123", "peto@example.com")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void registerShouldReturn400WithInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("peto", "jelszo123", "ez-nem-email")))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    // --- POST /api/auth/login ---

    @Test
    void loginShouldReturn200WithTokenAndUserForValidCredentials() throws Exception {
        when(userService.authenticate("peto", "jelszo123")).thenReturn(sampleUser);
        when(jwtTokenProvider.createToken("peto")).thenReturn("mock.jwt.token");

        LoginRequest req = new LoginRequest();
        req.setUsername("peto");
        req.setPassword("jelszo123");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                .andExpect(jsonPath("$.user.username").value("peto"))
                .andExpect(jsonPath("$.user.password").doesNotExist());
    }

    @Test
    void loginShouldReturn401ForInvalidCredentials() throws Exception {
        when(userService.authenticate(any(), any()))
                .thenThrow(new InvalidCredentialsException("Invalid username or password"));

        LoginRequest req = new LoginRequest();
        req.setUsername("peto");
        req.setPassword("rossz-jelszo");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void loginShouldReturn400WhenUsernameIsMissing() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setPassword("jelszo123");
        // username is intentionally missing.

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }
}