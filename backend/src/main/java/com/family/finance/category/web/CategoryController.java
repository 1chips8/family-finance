package com.family.finance.category.web;

import com.family.finance.category.dto.*;
import com.family.finance.category.service.CategoryService;
import com.family.finance.common.web.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) { this.categoryService = categoryService; }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list(@RequestParam(defaultValue = "true") boolean includeInactive) {
        return ApiResponse.of(categoryService.list(includeInactive));
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CreateCategoryRequest request) {
        return ApiResponse.of(categoryService.create(request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        return ApiResponse.of(categoryService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CategoryResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody UpdateCategoryStatusRequest request) {
        return ApiResponse.of(categoryService.updateStatus(id, request));
    }
}
