package com.financetracker.controller;

import com.financetracker.dto.CategoryExpense;
import com.financetracker.entity.User;
import com.financetracker.service.TransactionService;
import com.financetracker.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Добавить

class AnalyticsController {

    private final TransactionService transactionService;
    private final UserService userService;

    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }

    @GetMapping("/expenses-by-category")
    public ResponseEntity<List<CategoryExpense>> getExpensesByCategory(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        User user = getCurrentUser(authentication);

        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        List<CategoryExpense> expenses = transactionService.getExpensesByCategory(user, startDate, endDate);
        return ResponseEntity.ok(expenses);
    }
}