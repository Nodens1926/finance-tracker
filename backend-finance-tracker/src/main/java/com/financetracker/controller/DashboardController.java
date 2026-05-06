package com.financetracker.controller;

import com.financetracker.dto.CategoryExpense;
import com.financetracker.dto.DashboardSummary;
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
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Добавить
public class DashboardController {

    private final TransactionService transactionService;
    private final UserService userService;

    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> getDashboardSummary(Authentication authentication) {
        User user = getCurrentUser(authentication);
        DashboardSummary summary = transactionService.getDashboardSummary(user);
        return ResponseEntity.ok(summary);
    }
}

