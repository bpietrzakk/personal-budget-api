package com.bpietrzak.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private Map<String, BigDecimal> expensesByCategory;
}
