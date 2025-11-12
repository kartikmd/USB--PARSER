package com.myorg.usbparser.service.implementation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.myorg.usbparser.model.Section;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Standalone runner to parse a PDF into Section objects, post-process, deduplicate and write JSONL output.
 *
 * Usage: run main with args: <input-pdf> <output-jsonl>
 *
 * - Attempts to call SectionPostProcessor.cleanSections(List<Section>) if the class/method exists.
 * - Always runs an extra dedup step (keeps the best Section per section_id).
 */
@Slf4j
public class UsbParserRunner {
    private final String docTitle;
    private final PdfBoxSectionExtractor extractor;

    public UsbParserRunner(String docTitle) {
        this.docTitle = docTitle;
        this.extractor = new PdfBoxSectionExtractor(docTitle);
    }

    /**
     * Run parsing and write cleaned output.
     *
     * @param pdfPath    input PDF file path
     * @param outputPath output jsonl file path
     * @throws IOException on IO errors
     */
    public void run(Path pdfPath, Path outputPath) throws IOException {
        File pdfFile = pdfPath.toFile();
        if (!pdfFile.exists()) {
            throw new FileNotFoundException("PDF not found: " + pdfFile.getAbsolutePath());
        }
        log.info("Parsing PDF: {}", pdfFile.getAbsolutePath());
        List<Section> sections = extractor.parse(pdfFile);
        log.info("Parsed {} sections", sections.size());

        // 1) Try to call SectionPostProcessor.cleanSections(...) if available.
        sections = callSectionPostProcessorIfAvailable(sections);

        // 2) Deduplicate sections by section_id with heuristics to pick the best entry.
        List<Section> deduped = deduplicateSectionsKeepBest(sections);

        // 3) Write JSONL output
        writeJsonl(deduped, outputPath.toFile());

        // 4) Log summary
        int removed = sections.size() - deduped.size();
        log.info("Wrote {} sections to {} (removed {} duplicate entries)", deduped.size(), outputPath, removed);
        log.info("Completed: parsed={}, final={}, removed={} -> {}", sections.size(), deduped.size(), removed, outputPath);
    }

    /**
     * Attempt to call SectionPostProcessor.cleanSections(List<Section>) reflectively.
     * If not available or fails, returns the original list unchanged.
     */
    @SuppressWarnings("unchecked")
    private List<Section> callSectionPostProcessorIfAvailable(List<Section> sections) {
        try {
            Class<?> cls = Class.forName("com.myorg.usbparser.service.SectionPostProcessor");
            try {
                // method signature: public static List<Section> cleanSections(List<Section> sections)
                java.lang.reflect.Method m = cls.getMethod("cleanSections", List.class);
                Object result = m.invoke(null, sections);
                if (result instanceof List) {
                    log.info("Called SectionPostProcessor.cleanSections successfully.");
                    return (List<Section>) result;
                } else {
                    log.warn("SectionPostProcessor.cleanSections did not return List — ignoring.");
                }
            } catch (NoSuchMethodException nsme) {
                log.warn("SectionPostProcessor.cleanSections(List) not found. Skipping call.");
            }
        } catch (ClassNotFoundException cnfe) {
            log.info("SectionPostProcessor class not found on classpath. Skipping post-processor call.");
        } catch (Throwable t) {
            log.warn("Error while invoking SectionPostProcessor.cleanSections — skipping and continuing. Cause: {}", t.toString());
        }
        return sections;
    }

    /**
     * Deduplicate by section_id. Heuristics to choose which Section to keep:
     * - Prefer a Section whose content is not the placeholder "[No extractable text ...]".
     * - Among those, prefer the one with longer content length.
     * - If equal, prefer smaller page number.
     *
     * This preserves the "best" representation for each section_id.
     */
    private List<Section> deduplicateSectionsKeepBest(List<Section> sections) {
        Map<String, Section> bestById = new LinkedHashMap<>(sections.size());
        for (Section s : sections) {
            if (s == null) continue;
            String id = s.getSectionId();
            if (id == null) {
                // generate a synthetic id (fallback) - use fullPath + page
                id = (s.getFullPath() == null ? UUID.randomUUID().toString() : s.getFullPath()) + "@p" + (s.getPage() == null ? "0" : s.getPage());
            }
            Section current = bestById.get(id);
            if (current == null) {
                bestById.put(id, s);
                continue;
            }
            Section winner = chooseBetterSection(current, s);
            if (winner != current) {
                bestById.put(id, winner);
            }
        }
        return new ArrayList<>(bestById.values());
    }

    /**
     * Compare two sections and return the better one.
     */
    private Section chooseBetterSection(Section a, Section b) {
        boolean aPlaceholder = isPlaceholderContent(a.getContent());
        boolean bPlaceholder = isPlaceholderContent(b.getContent());
        if (aPlaceholder != bPlaceholder) {
            return aPlaceholder ? b : a;
        }
        int aLen = safeLength(a.getContent());
        int bLen = safeLength(b.getContent());
        if (aLen != bLen) {
            return (aLen > bLen) ? a : b;
        }
        // tie-breaker: prefer lower (earlier) page number if present
        Integer aPage = a.getPage();
        Integer bPage = b.getPage();
        if (aPage != null && bPage != null) {
            return aPage <= bPage ? a : b;
        } else if (aPage != null) {
            return a;
        } else if (bPage != null) {
            return b;
        }
        // final fallback: keep the first (a)
        return a;
    }

    private boolean isPlaceholderContent(String c) {
        if (c == null) return true;
        String trimmed = c.trim();
        return trimmed.isEmpty() || trimmed.startsWith("[No extractable text");
    }

    private int safeLength(String s) {
        return s == null ? 0 : s.trim().length();
    }

    /**
     * Write the list as newline-delimited JSON (JSONL) using Jackson.
     */
    private void writeJsonl(List<Section> sections, File outFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // Use a streaming generator for JSONL
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outFile))) {
            for (Section s : sections) {
                String json = mapper.writeValueAsString(s);
                bw.write(json);
                bw.newLine();
            }
        }
    }

    /* ----------------- main for quick testing ----------------- */
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            log.error("Usage: UsbParserRunner <input-pdf> [output-jsonl]");
            System.exit(2);
        }
        Path pdf = Path.of(args[0]);
        Path out = (args.length >= 2) ? Path.of(args[1]) : Path.of("usb_pd_sections_clean.jsonl");
        UsbParserRunner runner = new UsbParserRunner("USB Power Delivery Specification Rev 3.2");
        runner.run(pdf, out);
    }
}
