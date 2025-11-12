package com.myorg.usbparser.service.processing;

import com.myorg.usbparser.model.Section;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple post-processor for Sections:
 *  1) deduplicate sections by section_id (keep the "best" entry)
 *  2) set missing parentId when parent can be inferred from sectionId
 *  3) sort result by page then sectionId
 */
public final class SectionPostProcessor {

    private SectionPostProcessor() {}

    /**
     * Clean sections: dedupe and infer parentId.
     */
    public static List<Section> cleanSections(List<Section> sections) {
        if (sections == null || sections.isEmpty()) return Collections.emptyList();

        // 1) Group by sectionId (null/empty ids kept but deduped by content length)
        Map<String, List<Section>> byId = sections.stream()
                .collect(Collectors.groupingBy(s -> s.getSectionId() == null ? "" : s.getSectionId()));

        // choose best section for each id
        Map<String, Section> best = new HashMap<>();
        for (Map.Entry<String, List<Section>> e : byId.entrySet()) {
            String id = e.getKey();
            List<Section> list = e.getValue();

            // pick the Section with the longest non-placeholder content; tie-breaker: lowest page
            Section winner = list.stream().max(Comparator.comparingInt((Section s) -> {
                        String c = s.getContent() == null ? "" : s.getContent().trim();
                        // penalize placeholder marker so real content wins
                        int placeholderPenalty = c.startsWith("[No extractable text") ? 0 : c.length();
                        return placeholderPenalty;
                    }).thenComparingInt(s -> s.getPage() == null ? Integer.MAX_VALUE : -s.getPage()) // prefer lower page
            ).orElse(list.get(0));

            best.put(id, winner);
        }

        // 2) Infer parentId when missing and numeric parent exists
        // Build id->section map for lookups
        Map<String, Section> idMap = new HashMap<>(best);
        List<Section> output = new ArrayList<>();

        for (Section s : best.values()) {
            String sid = s.getSectionId();
            String parentId = s.getParentId();

            if ((parentId == null || parentId.trim().isEmpty()) && sid != null && sid.contains(".")) {
                // attempt to infer: remove last dot segment
                String inferred = sid.substring(0, sid.lastIndexOf('.'));
                if (idMap.containsKey(inferred)) {
                    parentId = inferred;
                } else {
                    // sometimes TOC split leads to e.g. "1.0 1.0 Initial..." - try trimming trailing zeros
                    String trimmed = tryTrimTrailingZeros(inferred);
                    if (trimmed != null && idMap.containsKey(trimmed)) {
                        parentId = trimmed;
                    } // else keep null
                }
            }

            // build a new Section object (Section is immutable-ish via builder)
            Section fixed = Section.builder()
                    .docTitle(s.getDocTitle())
                    .sectionId(s.getSectionId())
                    .title(s.getTitle())
                    .page(s.getPage())
                    .level(s.getLevel())
                    .parentId(parentId)
                    .fullPath(s.getFullPath())
                    .tags(s.getTags() == null ? List.of() : s.getTags())
                    .content(s.getContent())
                    .build();

            output.add(fixed);
        }

        // 3) Sort by page (nulls at end) then sectionId lexicographically
        output.sort(Comparator
                .comparing((Section s) -> s.getPage() == null ? Integer.MAX_VALUE : s.getPage())
                .thenComparing(s -> s.getSectionId() == null ? "" : s.getSectionId()));

        return output;
    }

    private static String tryTrimTrailingZeros(String id) {
        // e.g. "1.0.1.0" -> "1.0.1" or "1.0" -> "1"
        String cur = id;
        while (cur.contains(".")) {
            if (cur.endsWith(".0")) {
                cur = cur.substring(0, cur.length() - 2);
            } else break;
            if (!cur.contains(".")) break;
        }
        return cur.isEmpty() ? null : cur;
    }
}
