package com.family.finance.recurring.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

public record RecurringGenerationResponse(
        String month,
        int created,
        int alreadyGenerated,
        int skipped,
        List<SkippedTemplate> skipReasons
) {
    public RecurringGenerationResponse {
        skipReasons = skipReasons == null ? List.of() : List.copyOf(skipReasons);
    }

    public record SkippedTemplate(Long templateId, String reason) {}

    @JsonProperty("generated")
    public int generated() { return created; }

    public int generatedCount() { return created; }

    public int alreadyGeneratedCount() { return alreadyGenerated; }

    public int skippedCount() { return skipped; }

    @JsonProperty("skippedReasons")
    public List<String> skippedReasons() {
        return skipReasons.stream().map(SkippedTemplate::reason).toList();
    }
}
