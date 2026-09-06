package com.family.finance.ledger.web;

import com.family.finance.common.web.ApiResponse;
import com.family.finance.ledger.domain.LedgerType;
import com.family.finance.ledger.dto.CreateEntryRequest;
import com.family.finance.ledger.dto.EntryPageResponse;
import com.family.finance.ledger.dto.EntryQuery;
import com.family.finance.ledger.dto.EntryImportCommitResponse;
import com.family.finance.ledger.dto.EntryImportPreviewRequest;
import com.family.finance.ledger.dto.EntryImportPreviewResponse;
import com.family.finance.ledger.dto.EntryImportRequest;
import com.family.finance.ledger.service.EntryCsvService;
import com.family.finance.ledger.service.LedgerService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/entries")
public class LedgerController {
    private final LedgerService ledgerService;
    private final EntryCsvService entryCsvService;

    public LedgerController(LedgerService ledgerService, EntryCsvService entryCsvService) {
        this.ledgerService = ledgerService;
        this.entryCsvService = entryCsvService;
    }

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

    @GetMapping(value = "/export", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) LedgerType type,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long memberId) {
        byte[] csv = entryCsvService.export(new EntryQuery(from, to, type, categoryId, memberId, 1, 100));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=family-ledger.csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(csv);
    }

    @PostMapping("/import/preview")
    public ApiResponse<EntryImportPreviewResponse> previewImport(
            @Valid @RequestBody EntryImportPreviewRequest request) {
        return ApiResponse.of(entryCsvService.preview(request.csv()));
    }

    @PostMapping("/import/commit")
    public ApiResponse<EntryImportCommitResponse> commitImport(@Valid @RequestBody EntryImportRequest request) {
        return ApiResponse.of(entryCsvService.commit(request));
    }
}
