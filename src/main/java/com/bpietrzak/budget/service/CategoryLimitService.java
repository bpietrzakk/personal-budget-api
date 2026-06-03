package com.bpietrzak.budget.service;

import com.bpietrzak.budget.dto.CategoryLimitRequest;
import com.bpietrzak.budget.dto.CategoryLimitResponse;
import com.bpietrzak.budget.exception.ConflictException;
import com.bpietrzak.budget.exception.ResourceNotFoundException;
import com.bpietrzak.budget.model.CategoryLimit;
import com.bpietrzak.budget.repository.CategoryLimitRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryLimitService {

    private final CategoryLimitRepository categoryLimitRepository;

    public List<CategoryLimitResponse> findAll() {
        return categoryLimitRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public CategoryLimitResponse create(CategoryLimitRequest request) {
        if (categoryLimitRepository.existsByCategory(request.getCategory())) {
            throw new ConflictException("Limit for category '" + request.getCategory() + "' already exists");
        }
        CategoryLimit limit = CategoryLimit.builder()
                .category(request.getCategory())
                .limitAmount(request.getLimitAmount())
                .build();
        return toResponse(categoryLimitRepository.save(limit));
    }

    public void delete(String category) {
        CategoryLimit limit = categoryLimitRepository.findByCategory(category)
                .orElseThrow(() -> new ResourceNotFoundException("No limit found for category: " + category));
        categoryLimitRepository.delete(limit);
    }

    private CategoryLimitResponse toResponse(CategoryLimit limit) {
        return CategoryLimitResponse.builder()
                .id(limit.getId())
                .category(limit.getCategory())
                .limitAmount(limit.getLimitAmount())
                .build();
    }
}
