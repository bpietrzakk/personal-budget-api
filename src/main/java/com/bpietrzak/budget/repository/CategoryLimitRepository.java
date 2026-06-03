package com.bpietrzak.budget.repository;

import com.bpietrzak.budget.model.CategoryLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CategoryLimitRepository extends JpaRepository<CategoryLimit, UUID> {

    Optional<CategoryLimit> findByCategory(String category);

    boolean existsByCategory(String category);
}
