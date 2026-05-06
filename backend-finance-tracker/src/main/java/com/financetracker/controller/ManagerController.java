package com.financetracker.controller;

import com.financetracker.dto.CategoryExpense;
import com.financetracker.dto.DashboardSummary;
import com.financetracker.entity.Transaction;
import com.financetracker.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/manager")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true") // Добавить
@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
public class ManagerController {

    private final TransactionService transactionService;

    @GetMapping("/users/{userId}/transactions")
    public ResponseEntity<List<Transaction>> getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Transaction> transactions;
        if (startDate != null && endDate != null) {
            transactions = transactionService.getUserTransactionsByDateRange(userId, startDate, endDate);
        } else {
            transactions = transactionService.getUserTransactions(userId);
        }
        return ResponseEntity.ok(transactions);
    }

    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<DashboardSummary> getUserSummary(@PathVariable Long userId) {
        return ResponseEntity.ok(transactionService.getDashboardSummary(userId));
    }

    @GetMapping("/users/{userId}/analytics")
    public ResponseEntity<List<CategoryExpense>> getUserAnalytics(
            @PathVariable Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now();

        return ResponseEntity.ok(transactionService.getExpensesByCategory(userId, startDate, endDate));
    }
}