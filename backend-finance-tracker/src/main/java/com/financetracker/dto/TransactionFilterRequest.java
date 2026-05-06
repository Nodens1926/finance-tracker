package com.financetracker.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TransactionFilterRequest {
    // Фильтрация (минимум 3 параметра)
    private String type; // INCOME или EXPENSE
    private String category;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    // Поиск по ключевым полям
    private String search; // Поиск в description, category

    // Сортировка (уже в Pageable)
    private String sortBy;
    private String sortDir;

    // Пагинация (уже в Pageable)
    private Integer page = 0;
    private Integer size = 10;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
}