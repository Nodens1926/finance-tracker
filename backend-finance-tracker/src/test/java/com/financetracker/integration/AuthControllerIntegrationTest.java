package com.financetracker.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.entity.Role;
import com.financetracker.entity.User;
import com.financetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        userRepository.flush();
    }

    @Test
    void registerUser_shouldCreateNewUser() throws Exception {
        Map<String, String> user = Map.of(
                "username", "newuser",
                "email", "new@test.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));
    }

    @Test
    void registerUser_withDuplicateUsername_shouldReturnError() throws Exception {
        User existingUser = new User();
        existingUser.setUsername("existing");
        existingUser.setEmail("existing@test.com");
        existingUser.setPassword(passwordEncoder.encode("pass"));
        existingUser.setRoles(Collections.singleton(Role.ROLE_USER));
        existingUser.setEnabled(true);
        userRepository.save(existingUser);

        Map<String, String> newUser = Map.of(
                "username", "existing",
                "email", "new@test.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newUser)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticateUser_withValidCredentials_shouldReturnTokens() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRoles(Collections.singleton(Role.ROLE_USER));
        user.setEnabled(true);
        userRepository.save(user);

        Map<String, String> loginRequest = Map.of(
                "username", "testuser",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @Test
    void authenticateUser_withInvalidPassword_shouldReturnUnauthorized() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRoles(Collections.singleton(Role.ROLE_USER));
        user.setEnabled(true);
        userRepository.save(user);

        Map<String, String> loginRequest = Map.of(
                "username", "testuser",
                "password", "wrongpassword"
        );

        mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCurrentUser_withValidToken_shouldReturnUserInfo() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRoles(Collections.singleton(Role.ROLE_USER));
        user.setEnabled(true);
        userRepository.save(user);

        String accessToken = obtainAccessToken("testuser", "password123");

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    private String obtainAccessToken(String username, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }
}