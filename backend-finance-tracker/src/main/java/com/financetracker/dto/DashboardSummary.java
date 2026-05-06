package com.financetracker.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DashboardSummary {
    private BigDecimal totalIncome;
    private BigDecimal totalExpense;
    private BigDecimal balance;

    public DashboardSummary(BigDecimal totalIncome, BigDecimal totalExpense) {
        this.totalIncome = totalIncome != null ? totalIncome : BigDecimal.ZERO;
        this.totalExpense = totalExpense != null ? totalExpense : BigDecimal.ZERO;
        this.balance = this.totalIncome.subtract(this.totalExpense);
    }
}