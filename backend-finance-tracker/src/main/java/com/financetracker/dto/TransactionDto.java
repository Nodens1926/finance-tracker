package com.financetracker.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionDto {
    private Long id;
    private BigDecimal amount;
    private String type;
    private String category;
    private LocalDate date;
    private String description;
}