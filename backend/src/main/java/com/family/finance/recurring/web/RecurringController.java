package com.family.finance.recurring.web;

import com.family.finance.common.web.ApiResponse;
import com.family.finance.recurring.dto.RecurringGenerationResponse;
import com.family.finance.recurring.dto.RecurringTemplateRequest;
import com.family.finance.recurring.dto.RecurringTemplateResponse;
import com.family.finance.recurring.dto.UpdateRecurringStatusRequest;
import com.family.finance.recurring.service.RecurringService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-templates")
public class RecurringController {
    private final RecurringService recurringService;

    public RecurringController(RecurringService recurringService) { this.recurringService = recurringService; }

    @GetMapping
    public ApiResponse<List<RecurringTemplateResponse>> list() { return ApiResponse.of(recurringService.list()); }

    @PostMapping
    public ApiResponse<RecurringTemplateResponse> create(@Valid @RequestBody RecurringTemplateRequest request) {
        return ApiResponse.of(recurringService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RecurringTemplateResponse> update(@PathVariable Long id,
                                                          @Valid @RequestBody RecurringTemplateRequest request) {
        return ApiResponse.of(recurringService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<RecurringTemplateResponse> updateStatus(@PathVariable Long id,
                                                               @Valid @RequestBody UpdateRecurringStatusRequest request) {
        return ApiResponse.of(recurringService.updateStatus(id, request.active()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        recurringService.delete(id);
        return ApiResponse.of(null);
    }

    @PostMapping("/generate")
    public ApiResponse<RecurringGenerationResponse> generate(@RequestParam String month) {
        return ApiResponse.of(recurringService.generate(month));
    }
}
