package com.family.finance.ledger.web;

import com.family.finance.common.web.ApiResponse;
import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.ledger.dto.CreateEntryRequest;
import com.family.finance.ledger.dto.EntryPageResponse;
import com.family.finance.ledger.dto.EntryQuery;
import com.family.finance.ledger.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/entries")
public class LedgerController {
    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) { this.ledgerService = ledgerService; }

    @GetMapping
    public ApiResponse<EntryPageResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) LedgerType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.of(ledgerService.list(new EntryQuery(from, to, type, categoryId, memberId, page, pageSize)));
    }

    @PostMapping
    public ApiResponse<com.family.finance.ledger.dto.EntryResponse> create(@Valid @RequestBody CreateEntryRequest request) {
        return ApiResponse.of(ledgerService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<com.family.finance.ledger.dto.EntryResponse> update(@PathVariable Long id,
                                                                            @Valid @RequestBody CreateEntryRequest request) {
        return ApiResponse.of(ledgerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        ledgerService.delete(id);
        return ApiResponse.of(null);
    }
}
