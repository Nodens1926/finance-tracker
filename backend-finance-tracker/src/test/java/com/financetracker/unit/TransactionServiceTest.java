package com.financetracker.unit;


import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import com.financetracker.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testTransaction = new Transaction();
        testTransaction.setId(100L);
        testTransaction.setUser(testUser);
        testTransaction.setAmount(new BigDecimal("500.00"));
        testTransaction.setType(TransactionType.EXPENSE);
        testTransaction.setCategory("Food");
        testTransaction.setDate(LocalDate.now());
        testTransaction.setDescription("Groceries");
    }

    @Test
    void getUserTransactions_byUser_shouldReturnList() {
        // given
        List<Transaction> expected = List.of(testTransaction);
        when(transactionRepository.findByUserOrderByDateDesc(testUser)).thenReturn(expected);

        // when
        List<Transaction> result = transactionService.getUserTransactions(testUser);

        // then
        assertEquals(1, result.size());
        assertEquals(testTransaction.getId(), result.get(0).getId());
        verify(transactionRepository, times(1)).findByUserOrderByDateDesc(testUser);
    }

    @Test
    void getUserTransactions_byUserId_shouldReturnList() {
        // given
        List<Transaction> expected = List.of(testTransaction);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(transactionRepository.findByUserOrderByDateDesc(testUser)).thenReturn(expected);

        // when
        List<Transaction> result = transactionService.getUserTransactions(1L);

        // then
        assertEquals(1, result.size());
        verify(userRepository).findById(1L);
        verify(transactionRepository).findByUserOrderByDateDesc(testUser);
    }

    @Test
    void getUserTransactions_byUserId_userNotFound_shouldThrowException() {
        // given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionService.getUserTransactions(999L));
        assertEquals("User not found", exception.getMessage());
    }

    @Test
    void createTransaction_shouldSetUserAndSave() {
        // given
        Transaction newTransaction = new Transaction();
        newTransaction.setAmount(new BigDecimal("100.00"));
        newTransaction.setType(TransactionType.INCOME);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // when
        Transaction result = transactionService.createTransaction(newTransaction, testUser);

        // then
        assertEquals(testUser, result.getUser());
        verify(transactionRepository).save(newTransaction);
    }

    @Test
    void updateTransaction_shouldUpdateFields() {
        // given
        Transaction updateDetails = new Transaction();
        updateDetails.setAmount(new BigDecimal("999.00"));
        updateDetails.setType(TransactionType.INCOME);
        updateDetails.setCategory("Salary");
        updateDetails.setDate(LocalDate.of(2024, 1, 1));
        updateDetails.setDescription("Updated desc");

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        // when
        Transaction result = transactionService.updateTransaction(100L, updateDetails, testUser);

        // then
        assertEquals(new BigDecimal("999.00"), result.getAmount());
        assertEquals(TransactionType.INCOME, result.getType());
        assertEquals("Salary", result.getCategory());
        assertEquals("Updated desc", result.getDescription());
        verify(transactionRepository).save(testTransaction);
    }

    @Test
    void updateTransaction_whenNotOwner_shouldThrowException() {
        // given
        User otherUser = new User();
        otherUser.setId(999L);

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionService.updateTransaction(100L, new Transaction(), otherUser));
        assertEquals("Access denied", exception.getMessage());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deleteTransaction_shouldDelete() {
        // given
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));

        // when
        transactionService.deleteTransaction(100L, testUser);

        // then
        verify(transactionRepository).delete(testTransaction);
    }

    @Test
    void deleteTransaction_whenNotOwner_shouldThrowException() {
        // given
        User otherUser = new User();
        otherUser.setId(999L);

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionService.deleteTransaction(100L, otherUser));
        assertEquals("Access denied", exception.getMessage());
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }

    @Test
    void getTransactionById_shouldReturnTransaction() {
        // given
        when(transactionRepository.findById(100L)).thenReturn(Optional.of(testTransaction));

        // when
        Transaction result = transactionService.getTransactionById(100L);

        // then
        assertNotNull(result);
        assertEquals(100L, result.getId());
    }

    @Test
    void getTransactionById_notFound_shouldThrowException() {
        // given
        when(transactionRepository.findById(999L)).thenReturn(Optional.empty());

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> transactionService.getTransactionById(999L));
        assertTrue(exception.getMessage().contains("Transaction not found"));
    }
}
