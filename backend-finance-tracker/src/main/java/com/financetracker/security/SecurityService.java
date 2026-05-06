package com.financetracker.security;

import com.financetracker.entity.Role;
import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionAttachment;
import com.financetracker.entity.User;
import com.financetracker.repository.TransactionAttachmentRepository;
import com.financetracker.repository.TransactionRepository;
import com.financetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("securityService")
@RequiredArgsConstructor
public class SecurityService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionAttachmentRepository attachmentRepository;

    public boolean isCurrentUser(Long userId) {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getId().equals(userId);
    }

    public boolean hasRole(Role role) {
        User currentUser = getCurrentUser();
        return currentUser != null && currentUser.getRoles().contains(role);
    }

    public boolean canAccessUserData(Long userId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        // Админ может всё
        if (currentUser.getRoles().contains(Role.ROLE_ADMIN)) {
            return true;
        }

        // Менеджер может просматривать данные других пользователей
        if (currentUser.getRoles().contains(Role.ROLE_MANAGER)) {
            return true;
        }

        // Обычный пользователь может только свои данные
        return currentUser.getId().equals(userId);
    }

    public boolean canModifyUserData(Long transactionId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        // АДМИН МОЖЕТ ВСЁ!
        if (currentUser.getRoles().contains(Role.ROLE_ADMIN)) {
            System.out.println("✅ Админ имеет право редактировать любую транзакцию");
            return true;
        }

        // Менеджер может редактировать ТОЛЬКО свои транзакции
        if (currentUser.getRoles().contains(Role.ROLE_MANAGER)) {
            Transaction transaction = transactionRepository.findById(transactionId)
                    .orElse(null);
            if (transaction == null) return false;

            boolean isOwner = transaction.getUser().getId().equals(currentUser.getId());
            System.out.println("Менеджер " + (isOwner ? "может" : "НЕ может") + " редактировать транзакцию " + transactionId);
            return isOwner;
        }

        // Обычный пользователь может редактировать ТОЛЬКО свои транзакции
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElse(null);
        if (transaction == null) return false;

        boolean isOwner = transaction.getUser().getId().equals(currentUser.getId());
        System.out.println("Пользователь " + (isOwner ? "может" : "НЕ может") + " редактировать транзакцию " + transactionId);
        return isOwner;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String username = authentication.getName();
        return userRepository.findByUsername(username).orElse(null);
    }

    public boolean canAccessAttachment(Long attachmentId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        if (currentUser.getRoles().contains(Role.ROLE_ADMIN)) {
            return true;
        }

        TransactionAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElse(null);
        if (attachment == null) return false;

        Transaction transaction = attachment.getTransaction();

        if (currentUser.getRoles().contains(Role.ROLE_MANAGER)) {
            return true; // Менеджеры могут просматривать файлы всех пользователей
        }

        return transaction.getUser().getId().equals(currentUser.getId());
    }

    public boolean canModifyAttachment(Long attachmentId) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return false;

        if (currentUser.getRoles().contains(Role.ROLE_ADMIN)) {
            return true;
        }

        TransactionAttachment attachment = attachmentRepository.findById(attachmentId)
                .orElse(null);
        if (attachment == null) return false;

        // Только владелец может удалить свой файл
        return attachment.getUploadedBy().getId().equals(currentUser.getId());
    }
}