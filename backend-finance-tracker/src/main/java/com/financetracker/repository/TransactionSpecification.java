package com.financetracker.repository;

import com.financetracker.entity.Transaction;
import com.financetracker.entity.TransactionType;
import com.financetracker.entity.User;
import org.springframework.data.jpa.domain.Specification;
import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> belongsToUser(User user) {
        return (root, query, cb) -> cb.equal(root.get("user"), user);
    }

    public static Specification<Transaction> hasType(String type) {
        return (root, query, cb) -> {
            if (type == null || type.isEmpty()) return cb.conjunction();
            return cb.equal(root.get("type"), TransactionType.valueOf(type));
        };
    }

    public static Specification<Transaction> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isEmpty()) return cb.conjunction();
            return cb.equal(root.get("category"), category);
        };
    }

    public static Specification<Transaction> amountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return cb.conjunction();
            if (min != null && max != null) return cb.between(root.get("amount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("amount"), min);
            return cb.lessThanOrEqualTo(root.get("amount"), max);
        };
    }

    public static Specification<Transaction> dateBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) return cb.conjunction();
            if (start != null && end != null) return cb.between(root.get("date"), start, end);
            if (start != null) return cb.greaterThanOrEqualTo(root.get("date"), start);
            return cb.lessThanOrEqualTo(root.get("date"), end);
        };
    }

    public static Specification<Transaction> searchInFields(String searchTerm) {
        return (root, query, cb) -> {
            if (searchTerm == null || searchTerm.isEmpty()) return cb.conjunction();
            String pattern = "%" + searchTerm.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("description")), pattern),
                    cb.like(cb.lower(root.get("category")), pattern)
            );
        };
    }
}