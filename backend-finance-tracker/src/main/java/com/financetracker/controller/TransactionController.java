package com.financetracker.controller;

import com.financetracker.dto.TransactionDto;
import com.financetracker.entity.Transaction;
import com.financetracker.entity.User;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.service.TransactionService;
import com.financetracker.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.financetracker.dto.TransactionFilterRequest;
import com.financetracker.dto.FileUploadResponse;
import com.financetracker.entity.TransactionAttachment;
import com.financetracker.repository.TransactionSpecification;
import com.financetracker.service.S3Service;
import com.financetracker.service.TransactionAttachmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final TransactionAttachmentService attachmentService;
    private final S3Service s3Service;
    private final TransactionRepository transactionRepository;

    private User getCurrentUser(Authentication authentication) {
        return userService.findByUsername(authentication.getName());
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Transaction>> getUserTransactions(
            Authentication authentication,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        User user = getCurrentUser(authentication);
        List<Transaction> transactions;

        if (startDate != null && endDate != null) {
            transactions = transactionService.getUserTransactionsByDateRange(user, startDate, endDate);
        } else {
            transactions = transactionService.getUserTransactions(user);
        }

        return ResponseEntity.ok(transactions);
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Transaction> createTransaction(
            @RequestBody Transaction transaction,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        Transaction created = transactionService.createTransaction(transaction, user);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@securityService.canModifyUserData(#id)")
    public ResponseEntity<Transaction> updateTransaction(
            @PathVariable Long id,
            @RequestBody Transaction transactionDetails,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        Transaction updated = transactionService.updateTransaction(id, transactionDetails, user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@securityService.canModifyUserData(#id)")
    public ResponseEntity<?> deleteTransaction(
            @PathVariable Long id,
            Authentication authentication) {

        User user = getCurrentUser(authentication);
        transactionService.deleteTransaction(id, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export")
    @PreAuthorize("isAuthenticated()")
    public void exportTransactions(
            Authentication authentication,
            HttpServletResponse response) throws IOException {

        User user = getCurrentUser(authentication);
        List<Transaction> transactions = transactionService.getUserTransactions(user);

        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=transactions.csv");

        response.getWriter().println("Date,Type,Category,Amount,Description");

        for (Transaction transaction : transactions) {
            response.getWriter().printf("%s,%s,%s,%.2f,%s%n",
                    transaction.getDate(),
                    transaction.getType(),
                    transaction.getCategory(),
                    transaction.getAmount(),
                    transaction.getDescription() != null ? transaction.getDescription() : "");
        }

        response.getWriter().flush();
    }

    @GetMapping("/filtered")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<Transaction>> getFilteredTransactions(
            Authentication authentication,
            @ModelAttribute TransactionFilterRequest filterRequest) {

        User user = getCurrentUser(authentication);

        // [ИЗМЕНЕНИЕ] Валидация параметров пагинации
        if (filterRequest.getPage() != null && filterRequest.getPage() < 0) {
            filterRequest.setPage(0);
        }

        if (filterRequest.getSize() != null) {
            if (filterRequest.getSize() < 1) {
                filterRequest.setSize(10);
            }
            if (filterRequest.getSize() > 100) {
                throw new IllegalArgumentException("Page size must not exceed 100");
            }
        }

        // [ИЗМЕНЕНИЕ] Валидация поля сортировки (white list)
        if (filterRequest.getSortBy() != null && !filterRequest.getSortBy().isEmpty()) {
            List<String> allowedSortFields = Arrays.asList("date", "amount", "category", "id");
            if (!allowedSortFields.contains(filterRequest.getSortBy())) {
                throw new IllegalArgumentException("Invalid sort field: " + filterRequest.getSortBy() +
                        ". Allowed fields: " + String.join(", ", allowedSortFields));
            }
        }

        Specification<Transaction> spec = Specification
                .allOf(
                        TransactionSpecification.belongsToUser(user),
                        TransactionSpecification.hasType(filterRequest.getType()),
                        TransactionSpecification.hasCategory(filterRequest.getCategory()),
                        TransactionSpecification.amountBetween(filterRequest.getMinAmount(), filterRequest.getMaxAmount()),
                        TransactionSpecification.dateBetween(filterRequest.getStartDate(), filterRequest.getEndDate()),
                        TransactionSpecification.searchInFields(filterRequest.getSearch())
                );

        Pageable pageable = createPageable(filterRequest);
        Page<Transaction> transactions = transactionRepository.findAll(spec, pageable);
        return ResponseEntity.ok(transactions);
    }

    private Pageable createPageable(TransactionFilterRequest filterRequest) {
        Sort sort = Sort.by("date").descending();

        if (filterRequest.getSortBy() != null && !filterRequest.getSortBy().isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(filterRequest.getSortDir())
                    ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, filterRequest.getSortBy());
        }

        int page = filterRequest.getPage() != null ? filterRequest.getPage() : 0;
        int size = filterRequest.getSize() != null ? filterRequest.getSize() : 10;

        return PageRequest.of(page, size, sort);
    }

    @PostMapping("/{transactionId}/attachments")
    @PreAuthorize("@securityService.canModifyUserData(#transactionId)")
    public ResponseEntity<FileUploadResponse> uploadAttachment(
            @PathVariable Long transactionId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description,
            Authentication authentication) throws IOException {

        validateFile(file);

        User user = getCurrentUser(authentication);
        Transaction transaction = transactionService.getTransactionById(transactionId);

        String fileKey = s3Service.uploadFile(file, "transactions/" + transactionId);

        TransactionAttachment attachment = attachmentService.createAttachment(
                transaction, user, file, fileKey, description);

        String downloadUrl = s3Service.generatePresignedUrl(fileKey);

        FileUploadResponse response = new FileUploadResponse(
                attachment.getId(),
                attachment.getFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                downloadUrl,
                "File uploaded successfully"
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{transactionId}/attachments")
    @PreAuthorize("@securityService.canAccessUserData(#transactionId)")
    public ResponseEntity<List<FileUploadResponse>> getTransactionAttachments(
            @PathVariable Long transactionId) {

        List<TransactionAttachment> attachments = attachmentService.getAttachmentsByTransaction(transactionId);

        List<FileUploadResponse> responses = attachments.stream()
                .map(att -> new FileUploadResponse(
                        att.getId(),
                        att.getFileName(),
                        att.getFileType(),
                        att.getFileSize(),
                        s3Service.generatePresignedUrl(att.getFileKey()),
                        null
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/attachments/{attachmentId}/download")
    @PreAuthorize("@securityService.canAccessAttachment(#attachmentId)")
    public ResponseEntity<Map<String, String>> downloadAttachment(
            @PathVariable Long attachmentId) {

        TransactionAttachment attachment = attachmentService.getAttachmentById(attachmentId);
        String downloadUrl = s3Service.generatePresignedUrl(attachment.getFileKey());

        return ResponseEntity.ok(Map.of("downloadUrl", downloadUrl));
    }

    @DeleteMapping("/attachments/{attachmentId}")
    @PreAuthorize("@securityService.canModifyAttachment(#attachmentId)")
    public ResponseEntity<?> deleteAttachment(@PathVariable Long attachmentId) {
        TransactionAttachment attachment = attachmentService.getAttachmentById(attachmentId);

        // [ИЗМЕНЕНИЕ] Сначала удаляем из S3
        try {
            s3Service.deleteFile(attachment.getFileKey());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file from storage: " + e.getMessage());
        }

        // [ИЗМЕНЕНИЕ] Потом из БД
        attachmentService.deleteAttachment(attachmentId);

        return ResponseEntity.ok().body(Map.of("message", "Attachment deleted successfully"));
    }

    private void validateFile(MultipartFile file) {
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new RuntimeException("File size exceeds 10MB limit");
        }

        // [ИЗМЕНЕНИЕ] Расширенный список разрешенных типов файлов
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList(
                "image/jpeg", "image/png", "image/gif", "image/webp",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "text/plain", "text/csv"
        );

        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new RuntimeException("File type not allowed. Allowed types: images, PDF, Word, Excel, text files");
        }
    }
}