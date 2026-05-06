package com.financetracker.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.entity.*;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class TransactionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private com.financetracker.service.S3Service s3Service;

    private User testUser;
    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        transactionRepository.deleteAll();
        userRepository.deleteAll();
        userRepository.flush();

        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@test.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRoles(Collections.singleton(Role.ROLE_USER));
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        accessToken = obtainAccessToken("testuser", "password123");
    }

    @Test
    void createTransaction_shouldSaveTransaction() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.50"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setCategory("Food");
        transaction.setDate(LocalDate.now());
        transaction.setDescription("Groceries");

        mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.50))
                .andExpect(jsonPath("$.type").value("EXPENSE"));
    }

    @Test
    void getUserTransactions_shouldReturnList() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("50.00"));
        transaction.setType(TransactionType.INCOME);
        transaction.setCategory("Salary");
        transaction.setDate(LocalDate.now());
        transaction.setUser(testUser);
        transactionRepository.save(transaction);

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].amount").value(50.00));
    }

    @Test
    void updateTransaction_asOwner_shouldUpdate() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setCategory("Food");
        transaction.setDate(LocalDate.now());
        transaction.setUser(testUser);
        transaction = transactionRepository.save(transaction);

        Transaction updated = new Transaction();
        updated.setAmount(new BigDecimal("200.00"));
        updated.setType(TransactionType.EXPENSE);
        updated.setCategory("Restaurant");
        updated.setDate(LocalDate.now());

        mockMvc.perform(put("/api/transactions/" + transaction.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200.00))
                .andExpect(jsonPath("$.category").value("Restaurant"));
    }

    @Test
    void deleteTransaction_asOwner_shouldDelete() throws Exception {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setCategory("Food");
        transaction.setDate(LocalDate.now());
        transaction.setUser(testUser);
        transaction = transactionRepository.save(transaction);

        mockMvc.perform(delete("/api/transactions/" + transaction.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
    }

    @Test
    void getFilteredTransactions_withPagination_shouldReturnPage() throws Exception {
        for (int i = 0; i < 15; i++) {
            Transaction t = new Transaction();
            t.setAmount(new BigDecimal("10.00"));
            t.setType(TransactionType.EXPENSE);
            t.setCategory("Test");
            t.setDate(LocalDate.now());
            t.setUser(testUser);
            transactionRepository.save(t);
        }

        mockMvc.perform(get("/api/transactions/filtered")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(10))
                .andExpect(jsonPath("$.totalElements").value(15));
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