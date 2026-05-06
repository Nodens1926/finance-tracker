package com.financetracker.service;

import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionAttachment;
import com.financetracker.entity.User;
import com.financetracker.repository.TransactionAttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionAttachmentService {

    private final TransactionAttachmentRepository attachmentRepository;

    @Transactional
    public TransactionAttachment createAttachment(Transaction transaction, User user,
                                                  MultipartFile file, String fileKey,
                                                  String description) {
        TransactionAttachment attachment = new TransactionAttachment();
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileKey(fileKey);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setTransaction(transaction);
        attachment.setUploadedBy(user);
        attachment.setDescription(description);

        return attachmentRepository.save(attachment);
    }

    public List<TransactionAttachment> getAttachmentsByTransaction(Long transactionId) {
        return attachmentRepository.findByTransactionId(transactionId);
    }

    public TransactionAttachment getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
    }

    @Transactional
    public void deleteAttachment(Long attachmentId) {
        attachmentRepository.deleteById(attachmentId);
    }

    @Transactional
    public void deleteAllAttachmentsByTransaction(Transaction transaction) {
        attachmentRepository.deleteByTransaction(transaction);
    }
}