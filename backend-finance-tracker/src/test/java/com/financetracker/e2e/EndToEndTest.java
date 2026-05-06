package com.financetracker.e2e;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class EndToEndTest {

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

    private String accessToken;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void test1_registerAndLogin() throws Exception {
        // Регистрация
        Map<String, String> user = Map.of(
                "username", "testuser",
                "email", "test@test.com",
                "password", "password123"
        );

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());

        // Логин
        Map<String, String> login = Map.of(
                "username", "testuser",
                "password", "password123"
        );

        MvcResult result = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String response = result.getResponse().getContentAsString();
        accessToken = objectMapper.readTree(response).get("accessToken").asText();
    }

    @Test
    void test2_createAndGetTransaction() throws Exception {
        // Сначала логинимся
        test1_registerAndLogin();

        // Создаём транзакцию
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.50"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setCategory("Food");
        transaction.setDate(LocalDate.now());
        transaction.setDescription("Groceries");

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        Long transactionId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Получаем список транзакций
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void test3_updateAndDeleteTransaction() throws Exception {
        // Логин и создание
        test1_registerAndLogin();

        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setType(TransactionType.EXPENSE);
        transaction.setCategory("Food");
        transaction.setDate(LocalDate.now());

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transaction)))
                .andExpect(status().isOk())
                .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Обновление
        Transaction updated = new Transaction();
        updated.setAmount(new BigDecimal("200.00"));
        updated.setType(TransactionType.EXPENSE);
        updated.setCategory("Restaurant");
        updated.setDate(LocalDate.now());

        mockMvc.perform(put("/api/transactions/" + id)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(200.00));

        // Удаление
        mockMvc.perform(delete("/api/transactions/" + id)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // Проверка что удалилось
        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void test4_filterTransactions() throws Exception {
        // Логин
        test1_registerAndLogin();

        // Создаём 3 транзакции
        for (int i = 1; i <= 3; i++) {
            Transaction t = new Transaction();
            t.setAmount(new BigDecimal("100.00"));
            t.setType(TransactionType.EXPENSE);
            t.setCategory("Food");
            t.setDate(LocalDate.now());

            mockMvc.perform(post("/api/transactions")
                            .header("Authorization", "Bearer " + accessToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(t)))
                    .andExpect(status().isOk());
        }

        // Фильтр по типу
        mockMvc.perform(get("/api/transactions/filtered")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("type", "EXPENSE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void test5_accessDeniedForOtherUserTransaction() throws Exception {
        // Создаём пользователя1
        Map<String, String> user1 = Map.of(
                "username", "user1",
                "email", "user1@test.com",
                "password", "pass123"
        );
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user1)))
                .andExpect(status().isOk());

        // Логин user1
        Map<String, String> login1 = Map.of("username", "user1", "password", "pass123");
        MvcResult result1 = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login1)))
                .andReturn();
        String token1 = objectMapper.readTree(result1.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Создаём транзакцию user1
        Transaction t = new Transaction();
        t.setAmount(new BigDecimal("100.00"));
        t.setType(TransactionType.EXPENSE);
        t.setCategory("Food");
        t.setDate(LocalDate.now());

        MvcResult createResult = mockMvc.perform(post("/api/transactions")
                        .header("Authorization", "Bearer " + token1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(t)))
                .andExpect(status().isOk())
                .andReturn();

        Long txId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id").asLong();

        // Создаём пользователя2
        Map<String, String> user2 = Map.of(
                "username", "user2",
                "email", "user2@test.com",
                "password", "pass123"
        );
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user2)))
                .andExpect(status().isOk());

        // Логин user2
        Map<String, String> login2 = Map.of("username", "user2", "password", "pass123");
        MvcResult result2 = mockMvc.perform(post("/api/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login2)))
                .andReturn();
        String token2 = objectMapper.readTree(result2.getResponse().getContentAsString())
                .get("accessToken").asText();

        // Попытка удалить чужую транзакцию -> 403 или 500
        mockMvc.perform(delete("/api/transactions/" + txId)
                        .header("Authorization", "Bearer " + token2))
                .andExpect(status().isForbidden());
    }
}