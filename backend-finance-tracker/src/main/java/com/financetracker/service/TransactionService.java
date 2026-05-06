package com.financetracker.service;

import com.financetracker.dto.CategoryExpense;
import com.financetracker.dto.DashboardSummary;
import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public List<Transaction> getUserTransactions(User user) {
        return transactionRepository.findByUserOrderByDateDesc(user);
    }

    public List<Transaction> getUserTransactions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserOrderByDateDesc(user);
    }

    public List<Transaction> getUserTransactionsByDateRange(User user, LocalDate startDate, LocalDate endDate) {
        return transactionRepository.findByUserAndDateBetweenOrderByDateDesc(user, startDate, endDate);
    }

    public List<Transaction> getUserTransactionsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return transactionRepository.findByUserAndDateBetweenOrderByDateDesc(user, startDate, endDate);
    }

    public Transaction createTransaction(Transaction transaction, User user) {
        transaction.setUser(user);
        return transactionRepository.save(transaction);
    }

    public Transaction updateTransaction(Long id, Transaction transactionDetails, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        transaction.setAmount(transactionDetails.getAmount());
        transaction.setType(transactionDetails.getType());
        transaction.setCategory(transactionDetails.getCategory());
        transaction.setDate(transactionDetails.getDate());
        transaction.setDescription(transactionDetails.getDescription());

        return transactionRepository.save(transaction);
    }

    public void deleteTransaction(Long id, User user) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }

        transactionRepository.delete(transaction);
    }

    public DashboardSummary getDashboardSummary(User user) {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = now.withDayOfMonth(1);
        LocalDate endOfMonth = now.withDayOfMonth(now.lengthOfMonth());

        BigDecimal totalIncome = transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                user, TransactionType.INCOME, startOfMonth, endOfMonth);
        BigDecimal totalExpense = transactionRepository.sumAmountByUserAndTypeAndDateBetween(
                user, TransactionType.EXPENSE, startOfMonth, endOfMonth);

        return new DashboardSummary(totalIncome, totalExpense);
    }

    public DashboardSummary getDashboardSummary(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return getDashboardSummary(user);
    }

    public List<CategoryExpense> getExpensesByCategory(User user, LocalDate startDate, LocalDate endDate) {
        List<Object[]> results = transactionRepository.findExpensesByCategory(user, startDate, endDate);

        BigDecimal totalExpense = results.stream()
                .map(result -> (BigDecimal) result[1])
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return results.stream()
                .map(result -> {
                    String category = (String) result[0];
                    BigDecimal amount = (BigDecimal) result[1];
                    Double percentage = totalExpense.compareTo(BigDecimal.ZERO) > 0 ?
                            amount.divide(totalExpense, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100 : 0.0;

                    CategoryExpense categoryExpense = new CategoryExpense(category, amount);
                    categoryExpense.setPercentage(percentage);
                    return categoryExpense;
                })
                .collect(Collectors.toList());
    }

    public List<CategoryExpense> getExpensesByCategory(Long userId, LocalDate startDate, LocalDate endDate) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return getExpensesByCategory(user, startDate, endDate);
    }

    // Добавьте этот метод в класс TransactionService
    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found with id: " + id));
    }
}