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
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        userRepository.flush();

        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRoles(Set.of(Role.ROLE_ADMIN));
        admin.setEnabled(true);
        userRepository.save(admin);

        User regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setEmail("user@test.com");
        regularUser.setPassword(passwordEncoder.encode("user123"));
        regularUser.setRoles(Set.of(Role.ROLE_USER));
        regularUser.setEnabled(true);
        userRepository.save(regularUser);

        adminToken = obtainAccessToken("admin", "admin123");
        userToken = obtainAccessToken("user", "user123");
    }

    @Test
    void getAllUsers_asAdmin_shouldReturnAllUsers() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].username").exists());
    }

    @Test
    void getAllUsers_asRegularUser_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserRoles_asAdmin_shouldUpdate() throws Exception {
        User user = userRepository.findByUsername("user").get();
        Set<String> roles = Set.of("ROLE_MANAGER");

        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/roles")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(roles)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles").isArray());
    }

//    @Test
//    void toggleUserStatus_asAdmin_shouldDisableUser() throws Exception {
//        User user = userRepository.findByUsername("user").get();
//
//        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/toggle-status")
//                        .header("Authorization", "Bearer " + adminToken)
//                        .param("enabled", "false"))
//                .andExpect(status().isOk());
//    }

//    @Test
//    void addRoleToUser_asAdmin_shouldAddRole() throws Exception {
//        User user = userRepository.findByUsername("user").get();
//
//        mockMvc.perform(post("/api/admin/users/" + user.getId() + "/roles/ROLE_MANAGER")
//                        .header("Authorization", "Bearer " + adminToken))
//                .andExpect(status().isOk());
//    }

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