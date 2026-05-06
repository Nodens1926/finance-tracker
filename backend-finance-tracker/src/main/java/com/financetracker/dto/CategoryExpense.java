package com.financetracker.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class CategoryExpense {
    private String category;
    private BigDecimal amount;
    private Double percentage;

    public CategoryExpense(String category, BigDecimal amount) {
        this.category = category;
        this.amount = amount;
    }
}