package com.bpietrzak.budget.controller;

import com.bpietrzak.budget.dto.CategoryLimitRequest;
import com.bpietrzak.budget.dto.CategoryLimitResponse;
import com.bpietrzak.budget.service.CategoryLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/limits")
@RequiredArgsConstructor
@Tag(name = "Category Limits", description = "Manage monthly spending limits per category")
public class CategoryLimitController {

    private final CategoryLimitService categoryLimitService;

    @GetMapping
    @Operation(summary = "List all category limits")
    public List<CategoryLimitResponse> getAll() {
        return categoryLimitService.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Set a monthly spending limit for a category")
    public CategoryLimitResponse create(@RequestBody @Valid CategoryLimitRequest request) {
        return categoryLimitService.create(request);
    }

    @DeleteMapping("/{category}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove spending limit for a category")
    public void delete(@PathVariable String category) {
        categoryLimitService.delete(category);
    }
}
