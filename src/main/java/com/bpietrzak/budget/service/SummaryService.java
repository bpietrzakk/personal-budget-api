package com.bpietrzak.budget.service;

import com.bpietrzak.budget.dto.SummaryResponse;
import com.bpietrzak.budget.model.enums.TransactionType;
import com.bpietrzak.budget.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TransactionRepository transactionRepository;

    public SummaryResponse getSummary(LocalDate from, LocalDate to) {
        BigDecimal totalIncome = transactionRepository.sumByType(TransactionType.INCOME, from, to);
        BigDecimal totalExpenses = transactionRepository.sumByType(TransactionType.EXPENSE, from, to);

        Map<String, BigDecimal> expensesByCategory = transactionRepository
                .sumExpensesByCategory(from, to)
                .stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                ));

        return SummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpenses)
                .expensesByCategory(expensesByCategory)
                .build();
    }
}
