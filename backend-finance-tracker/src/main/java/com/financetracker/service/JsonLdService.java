package com.financetracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.financetracker.entity.Transaction;
import com.financetracker.entity.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class JsonLdService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String generateTransactionJsonLd(Transaction transaction, User user) {
        Map<String, Object> jsonLd = new HashMap<>();
        jsonLd.put("@context", "https://schema.org");
        jsonLd.put("@type", "FinancialProduct");
        jsonLd.put("name", transaction.getCategory() + " Transaction");
        jsonLd.put("description", transaction.getDescription());
        jsonLd.put("amount", transaction.getAmount());
        jsonLd.put("currency", "USD");
        jsonLd.put("date", transaction.getDate().toString());

        Map<String, Object> userMap = new HashMap<>();
        userMap.put("@type", "Person");
        userMap.put("name", user.getUsername());
        jsonLd.put("accountablePerson", userMap);

        try {
            return objectMapper.writeValueAsString(jsonLd);
        } catch (Exception e) {
            return "{}";
        }
    }
}