package com.ipetoi.taskmanager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Smoke test: verifies that the application starts successfully and basic
 * endpoints respond before running any detailed tests.
 * If this fails, there is likely a fundamental configuration or startup issue.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
class SmokeTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
        // Verify that the Spring context starts successfully (beans, configuration, JPA).
    }

    @Test
    void staticIndexPageIsReachable() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    void registerEndpointIsReachable() throws Exception {
        // Sending invalid data intentionally; we only verify that the endpoint responds
        // (not 404 or 500 - a validation 400 response is acceptable).
        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginEndpointIsReachable() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void tasksEndpointRequiresAuthentication() throws Exception {
        // Without a token, the response should be 401 or 403 (depending on the Spring Security version).
        // The important part is that it is not 200 or 500.
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().is4xxClientError());
    }
}