package com.myorg.usbparser.model;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private int tocSectionCount;
    private int parsedSectionCount;

    @Builder.Default
    private List<String> missingSections = List.of();

    @Builder.Default
    private List<String> extraSections = List.of();

    @Builder.Default
    private Map<String, Integer> tableCounts = Map.of();

    private int missingCount;
    private int extraCount;

    // Defensive getters to avoid exposing internal mutable collections
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
