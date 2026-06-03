package com.bpietrzak.budget.repository;

import com.bpietrzak.budget.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    boolean existsByAccountId(UUID accountId);

    @Query("SELECT t FROM Transaction t WHERE " +
           "(:from IS NULL OR t.transactionDate >= :from) AND " +
           "(:to IS NULL OR t.transactionDate <= :to) AND " +
           "(:category IS NULL OR t.category = :category)")
    List<Transaction> findWithFilters(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("category") String category
    );
}
