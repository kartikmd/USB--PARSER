package com.myorg.usbparser.service.implementation;

import com.myorg.usbparser.model.Section;
import com.myorg.usbparser.service.TocExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PdfBox TOC extractor — final improved version.
 *
 * Improvements vs earlier:
 *  - aggressive cleanup of dot-leaders and trailing page tokens in titles
 *  - recovers page numbers appended to title text
 *  - ignores obvious years used in front-matter
 *  - deduplicates by sectionId (first good record kept; prefer non-zero page)
 *  - joins lines when the numeric id is on one line and title on the next
 *  - guarded against pages outside doc length
 */
@Slf4j
public class PdfBoxTocExtractor implements TocExtractor {

    private static final Pattern DOTS_PAGE = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)*)\\s+(.+?)\\s*\\.{2,}\\s*(\\d{1,4})\\s*$");
    private static final Pattern TITLE_PAGE = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)*)\\s+(.+?)\\s+(\\d{1,4})\\s*$");
    private static final Pattern NUMBER_TITLE = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)*)\\s+(.+?)\\s*$");
    private static final Pattern TRAILING_PAGE_IN_TITLE = Pattern.compile("(.+?)\\s*\\.*\\s*(\\d{1,4})\\s*$");
    private static final Pattern ONLY_NUMBER = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)*)\\s*$");
    private static final Pattern REVISION_KEYWORDS = Pattern.compile(
            "\\b(errata|erratum|revision|revision history|including errata|ecn|ecns|editorial changes|initial release|change log)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MONTH_WORD = Pattern.compile(
            "\\b(jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\b",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_DOTS = Pattern.compile("\\.{2,}\\s*$");
    private static final Pattern TRAILING_PAGE_NUMBER = Pattern.compile("\\s+\\d{1,4}\\s*$");

    private final String docTitle;

    public PdfBoxTocExtractor(String docTitle) {
        this.docTitle = docTitle;
    }

    @Override
    public List<Section> parse(File pdfFile) throws IOException {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new IOException("PDF file is null or does not exist: " + (pdfFile == null ? "null" : pdfFile.getAbsolutePath()));
        }

        Map<String, Section> byId = new LinkedHashMap<>(); // preserve order, dedupe by id preferring first valid

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            int docPages = document.getNumberOfPages();
            int currentYear = Calendar.getInstance().get(Calendar.YEAR);

            // Read first N pages of the document where TOC usually lives.
            int maxTocPages = Math.min(60, Math.max(5, docPages));
            stripper.setStartPage(1);
            stripper.setEndPage(maxTocPages);
            String raw = normalize(stripper.getText(document));

            String[] lines = raw.split("\\r?\\n");
            List<String> cleanedLines = new ArrayList<>();

            // Join lines where a numeric id is alone, then next line is title or dotted line
            for (int i = 0; i < lines.length; i++) {
                String l = lines[i].trim();
                if (l.isEmpty()) continue;

                Matcher mNumOnly = ONLY_NUMBER.matcher(l);
                if (mNumOnly.matches() && i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (!next.isEmpty()) {
                        cleanedLines.add(l + " " + next);
                        i++; // skip next
                        continue;
                    }
                }

                // If line ends with dot leaders but page on next line, join them
                if (TRAILING_DOTS.matcher(l).find() && i + 1 < lines.length) {
                    String next = lines[i + 1].trim();
                    if (next.matches("^\\d{1,4}$")) {
                        cleanedLines.add(l + " " + next);
                        i++;
                        continue;
                    }
                }

                cleanedLines.add(l);
            }

            boolean tocStarted = false;

            for (String line : cleanedLines) {
                if (line == null || line.trim().isEmpty()) continue;
                // skip obviously editorial / errata lines until we detect ToC start
                String low = line.toLowerCase();
                if (!tocStarted) {
                    if (REVISION_KEYWORDS.matcher(low).find() || MONTH_WORD.matcher(low).find()) {
                        // skip preface front-matter lines
                        continue;
                    }
                }

                try {
                    // 1) dotted leaders with page
                    Matcher m = DOTS_PAGE.matcher(line);
                    if (m.matches()) {
                        String id = m.group(1).trim();
                        String rawTitle = m.group(2).trim();
                        int pg = safeParseInt(m.group(3));
                        pg = validateCandidatePage(pg, docPages, currentYear);
                        String title = cleanTitle(rawTitle);

                        if (!tocStarted && pg > 0) tocStarted = true;
                        if (!tocStarted) continue;

                        putBest(byId, id, title, pg);
                        continue;
                    }

                    // 2) title + page without dotted leaders
                    m = TITLE_PAGE.matcher(line);
                    if (m.matches()) {
                        String id = m.group(1).trim();
                        String rawTitle = m.group(2).trim();
                        int pg = safeParseInt(m.group(3));
                        pg = validateCandidatePage(pg, docPages, currentYear);
                        String title = cleanTitle(rawTitle);

                        if (!tocStarted && pg > 0) tocStarted = true;
                        if (!tocStarted) continue;

                        putBest(byId, id, title, pg);
                        continue;
                    }

                    // 3) number + title, maybe page appended inside title (without dots)
                    m = NUMBER_TITLE.matcher(line);
                    if (m.matches()) {
                        String id = m.group(1).trim();
                        String rawTitle = m.group(2).trim();
                        // try to find trailing page in title
                        Matcher trailing = TRAILING_PAGE_IN_TITLE.matcher(rawTitle);
                        if (trailing.matches()) {
                            String titlePart = trailing.group(1).trim();
                            int pg = safeParseInt(trailing.group(2));
                            pg = validateCandidatePage(pg, docPages, currentYear);
                            String title = cleanTitle(titlePart);
                            if (pg > 0) {
                                if (!tocStarted && pg > 0) tocStarted = true;
                                if (!tocStarted) continue;
                                putBest(byId, id, title, pg);
                                continue;
                            } else {
                                // remove the trailing number anyway
                                rawTitle = cleanTitle(titlePart);
                            }
                        } else {
                            rawTitle = cleanTitle(rawTitle);
                        }

                        if (!tocStarted) {
                            // heuristics: if this looks like TOC starter, mark started
                            if (rawTitle.toLowerCase().matches(".*(contents|table of contents|introduction|overview).*")) {
                                tocStarted = true;
                            } else {
                                // skip non-TOC lines until we identify toc start
                                continue;
                            }
                        }

                        putBest(byId, id, rawTitle, 0);
                        continue;
                    }

                    // fallback: sometimes line contains title then dots and page separated by multiple spaces
                    // attempt small heuristic: last token numeric
                    String[] tokens = line.trim().split("\\s+");
                    String last = tokens[tokens.length - 1];
                    if (last.matches("\\d{1,4}")) {
                        // find first token that looks like section id (start of line)
                        String[] parts = line.split("\\s+", 2);
                        if (parts.length >= 2 && parts[0].matches("\\d+(?:\\.\\d+)*")) {
                            String id = parts[0].trim();
                            String rest = parts[1].trim();
                            int pg = safeParseInt(last);
                            pg = validateCandidatePage(pg, docPages, currentYear);
                            String title = cleanTitle(rest.replaceAll("\\d{1,4}$", ""));
                            if (!tocStarted && pg > 0) tocStarted = true;
                            if (!tocStarted) continue;
                            putBest(byId, id, title, pg);
                            continue;
                        }
                    }

                } catch (Exception ex) {
                    log.debug("line parse non-fatal: '{}' -> {}", line, ex.toString());
                }
            }

            // final: convert map to list preserving appearance order
            List<Section> sections = new ArrayList<>();
            for (Map.Entry<String, Section> e : byId.entrySet()) {
                sections.add(e.getValue());
            }

            log.info("✅ Extracted {} TOC entries from {} (pages scanned: {})", sections.size(), pdfFile.getName(), maxTocPages);
            return sections;
        } catch (Exception e) {
            log.error("Error extracting TOC: {}", e.toString(), e);
            throw new IOException("Error extracting TOC", e);
        }
    }

    /** Insert or update the dedupe map, preferring the first entry with a valid page or non-empty title. */
    private void putBest(Map<String, Section> map, String sectionId, String title, int page) {
        if (sectionId == null || sectionId.isBlank()) return;
        Section existing = map.get(sectionId);
        if (existing == null) {
            map.put(sectionId, buildSection(sectionId, title, page));
            return;
        }
        // prefer entry that has non-zero page
        if (existing.getPage() == 0 && page > 0) {
            map.put(sectionId, buildSection(sectionId, title.isBlank() ? existing.getTitle() : title, page));
            return;
        }
        // prefer non-empty title if existing is empty
        if ((existing.getTitle() == null || existing.getTitle().isBlank()) && (title != null && !title.isBlank())) {
            map.put(sectionId, buildSection(sectionId, title, existing.getPage()));
        }
        // otherwise keep existing (first wins)
    }

    private Section buildSection(String sectionId, String title, int page) {
        int level = sectionId.contains(".") ? sectionId.split("\\.").length : 1;
        String parentId = sectionId.contains(".") ? sectionId.substring(0, sectionId.lastIndexOf('.')) : null;
        String fullPath = (sectionId + " " + (title == null ? "" : title)).trim();
        return Section.builder()
                .docTitle(docTitle)
                .sectionId(sectionId)
                .title(title == null ? "" : title)
                .page(page)
                .level(level)
                .parentId(parentId)
                .fullPath(fullPath)
                .tags(new ArrayList<>())
                .build();
    }

    private static int safeParseInt(String raw) {
        if (raw == null) return 0;
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private static int validateCandidatePage(int candidate, int docPages, int currentYear) {
        if (candidate <= 0) return 0;
        // if candidate looks like a year, ignore it
        if (candidate >= 1900 && candidate <= (currentYear + 1)) return 0;
        // if candidate is beyond doc length, ignore
        if (docPages > 0 && candidate > docPages) return 0;
        return candidate;
    }

    private static String cleanTitle(String raw) {
        if (raw == null) return "";
        String t = raw.trim();
        // remove any leading section id that sneaked into title
        t = t.replaceFirst("^\\s*\\d+(?:\\.\\d+)*\\s+", "");
        // remove dot leaders like "......" and replace with single space
        t = t.replaceAll("\\.{2,}", " ");
        // remove trailing page numbers (e.g. "Title 34" -> "Title")
        t = t.replaceAll("\\s+\\d{1,4}$", "");
        // remove stray punctuation at end
        t = t.replaceAll("[\\p{Punct}\\s]+$", "");
        // normalize multiple spaces
        t = t.replaceAll("\\s{2,}", " ").trim();
        return t;
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC)
                .replace('\u00A0', ' ')
                .replaceAll("\\u200B", "")
                .replaceAll("[\\t\\x0B\\f\\r]+", " ");
        StringBuilder out = new StringBuilder();
        for (String line : n.split("\\r?\\n")) {
            String t = line.replaceAll(" {2,}", " ").trim();
            out.append(t).append('\n');
        }
        return out.toString();
    }
}
