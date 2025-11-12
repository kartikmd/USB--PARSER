package com.myorg.usbparser.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Map;

/**
 * Validation summary for parsed USB PD specification results.
 * Used for Excel validation reports.
 */
@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationResult {

    // --- counts ---
    @JsonProperty("toc_count")
    private Integer tocSectionCount;

    @JsonProperty("parsed_count")
    private Integer parsedSectionCount;

    @JsonProperty("missing_count")
    private Integer missingCount;

    @JsonProperty("extra_count")
    private Integer extraCount;

    // --- lists (used by Excel sheet “Missing Sections” / “Extra Sections”) ---
    @JsonProperty("missing_sections")
    private List<String> missingSections;

    @JsonProperty("extra_sections")
    private List<String> extraSections;

    // --- optional table stats ---
    // keys recommended:
    // "toc_tables_total", "parsed_tables_total"
    // optionally "toc_table_titles", "parsed_table_titles"
    @JsonProperty("table_counts")
    private Map<String, Integer> tableCounts;

    // Defensive getters to preserve immutability
    public List<String> getMissingSections() {
        return missingSections == null ? List.of() : List.copyOf(missingSections);
    }

    public List<String> getExtraSections() {
        return extraSections == null ? List.of() : List.copyOf(extraSections);
    }

    public Map<String, Integer> getTableCounts() {
        return tableCounts == null ? Map.of() : Map.copyOf(tableCounts);
    }
}
