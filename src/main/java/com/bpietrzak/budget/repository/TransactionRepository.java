package com.bpietrzak.budget.repository;

import com.bpietrzak.budget.model.Transaction;
import com.bpietrzak.budget.model.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    boolean existsByAccountId(UUID accountId);

    List<Transaction> findByAccountIdOrderByTransactionDateDesc(UUID accountId);

    @Query("SELECT t FROM Transaction t WHERE " +
           "t.transactionDate >= COALESCE(:from, t.transactionDate) AND " +
           "t.transactionDate <= COALESCE(:to, t.transactionDate) AND " +
           "(:category IS NULL OR t.category = :category)")
    List<Transaction> findWithFilters(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("category") String category
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = :type AND " +
           "t.transactionDate >= COALESCE(:from, t.transactionDate) AND " +
           "t.transactionDate <= COALESCE(:to, t.transactionDate)")
    BigDecimal sumByType(
            @Param("type") TransactionType type,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.type = 'EXPENSE' AND " +
           "t.category = :category AND " +
           "YEAR(t.transactionDate) = YEAR(CURRENT_DATE) AND MONTH(t.transactionDate) = MONTH(CURRENT_DATE)")
    BigDecimal sumExpensesThisMonthByCategory(@Param("category") String category);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t WHERE t.type = 'EXPENSE' AND " +
           "t.transactionDate >= COALESCE(:from, t.transactionDate) AND " +
           "t.transactionDate <= COALESCE(:to, t.transactionDate) " +
           "GROUP BY t.category")
    List<Object[]> sumExpensesByCategory(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
