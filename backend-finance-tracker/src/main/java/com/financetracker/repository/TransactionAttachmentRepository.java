package com.financetracker.repository;

import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionAttachmentRepository extends JpaRepository<TransactionAttachment, Long> {
    List<TransactionAttachment> findByTransaction(Transaction transaction);
    List<TransactionAttachment> findByTransactionId(Long transactionId);
    Optional<TransactionAttachment> findByFileKey(String fileKey);
    void deleteByTransaction(Transaction transaction);
}