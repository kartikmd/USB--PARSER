package com.myorg.usbparser.service.implementation;

import com.myorg.usbparser.model.Section;
import com.myorg.usbparser.service.SectionExtractor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PdfBoxSectionExtractor implements SectionExtractor {

    // headings like "1.2.3 Title" or "1 Title" (need at least one letter in title)
    private static final Pattern HEADING_PATTERN = Pattern.compile(
            "^\\s*(\\d+(?:\\.\\d+)*)\\s+(?=.+[A-Za-z])(.+?)\\s*$"
    );

    private static final Pattern IGNORE_CONTENT = Pattern.compile(
            "^(Figure\\s+\\d+|Table\\s+\\d+|List of Figures|List of Tables|Revision History)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PRINTED_PAGE_AT_LINE_END = Pattern.compile(".*\\b(\\d{1,4})\\s*$");

    // typical header/footer lines to ignore (case-insensitive)
    private static final Pattern PAGE_FURNITURE = Pattern.compile(
            "(?i)^(page\\s+\\d+|universal serial bus power delivery specification|revision history)$"
    );

    private final String docTitle;

    public PdfBoxSectionExtractor(String docTitle) {
        this.docTitle = docTitle;
    }

    @Override
    public List<Section> parse(File pdfFile) throws IOException {
        Objects.requireNonNull(pdfFile, "pdfFile must not be null");
        if (!pdfFile.exists()) throw new IOException("PDF file does not exist: " + pdfFile.getAbsolutePath());

        List<Section> sections = new ArrayList<>();

        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            // Improve extraction ordering for multi-column / layouted PDFs
            stripper.setSortByPosition(true);

            int totalPages = document.getNumberOfPages();
            int[] printed = new int[totalPages + 1]; // 1-based

            // First pass: try to detect printed page numbers from last non-empty line
            for (int p = 1; p <= totalPages; p++) {
                stripper.setStartPage(p);
                stripper.setEndPage(p);
                String pageText = normalize(stripper.getText(document));
                if (pageText == null) pageText = "";
                String[] lines = pageText.split("\\r?\\n");
                for (int i = lines.length - 1; i >= 0; i--) {
                    String l = lines[i] == null ? "" : lines[i].trim();
                    if (l.isEmpty()) continue;
                    // avoid matching dotted leaders "..... 53" in section title
                    if (l.matches(".*\\.{2,}\\s*\\d+\\s*$")) {
                        // likely a ToC dotted leader, skip as printed page candidate
                        break;
                    }
                    Matcher m = PRINTED_PAGE_AT_LINE_END.matcher(l);
                    if (m.matches()) {
                        try {
                            int val = Integer.parseInt(m.group(1));
                            if (val > 0 && val < 10000) {
                                printed[p] = val;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    break;
                }
            }

            // Decide whether printed[] is reliable: check monotonic run
            boolean usePrinted = shouldUsePrintedPages(printed);

            if (!usePrinted) {
                // zero out to make logic simple downstream
                Arrays.fill(printed, 0);
                log.debug("Printed page numbers ignored due to inconsistency.");
            } else {
                log.debug("Using printed page numbers detected on pages.");
            }

            Section current = null;
            StringBuilder buf = new StringBuilder();

            for (int page = 1; page <= totalPages; page++) {
                stripper.setStartPage(page);
                stripper.setEndPage(page);
                String text = normalize(stripper.getText(document));
                if (text == null) text = "";
                String[] lines = text.split("\\r?\\n");

                // merge numeric-only id lines with next line (common in broken PDFs)
                for (int i = 0; i < lines.length; i++) {
                    String trimmed = lines[i] == null ? "" : lines[i].trim();
                    if (trimmed.matches("^\\d+(?:\\.\\d+)*$") && i + 1 < lines.length) {
                        String next = lines[i + 1] == null ? "" : lines[i + 1].trim();
                        if (next.startsWith("-")) next = next.substring(1).trim();
                        lines[i + 1] = (trimmed + " " + next).trim();
                        lines[i] = "";
                    }
                }

                for (String raw : lines) {
                    if (raw == null) continue;
                    String line = raw.trim();
                    if (line.isEmpty()) continue;

                    // early filter of likely headers/footers
                    if (PAGE_FURNITURE.matcher(line).find()) continue;

                    Matcher m = HEADING_PATTERN.matcher(line);
                    if (m.matches()) {
                        // finalize previous section (immutable)
                        if (current != null) {
                            sections.add(finalizeSection(current, buf));
                        }

                        String sectionId = m.group(1).trim();
                        String title = m.group(2).trim();

                        // Clean title: remove dotted leaders and trailing page numbers
                        title = title.replaceAll("\\.{2,}\\s*\\d+$", "").trim();
                        title = title.replaceAll("\\s+\\d+$", "").trim();
                        // collapse runs of dots or excessive spaces
                        title = title.replaceAll("[\\.\\s]{2,}", " ").trim();

                        int level = sectionId.split("\\.").length;
                        String parentId = sectionId.contains(".")
                                ? sectionId.substring(0, sectionId.lastIndexOf('.'))
                                : null;

                        Integer pageToSet = null;
                        if (usePrinted && page <= printed.length - 1 && printed[page] > 0) {
                            pageToSet = Integer.valueOf(printed[page]);
                        } else {
                            // use pdf page index as fallback (but allow null if you prefer)
                            pageToSet = Integer.valueOf(page);
                        }

                        log.debug("Heading pdf#{} printed#{} -> {} {}", page,
                                (page <= printed.length - 1 ? printed[page] : 0), sectionId, title);

                        // Build a new Section (immutable)
                        current = Section.builder()
                                .docTitle(docTitle)
                                .sectionId(sectionId)
                                .title(title)
                                .page(pageToSet)
                                .level(level)
                                .parentId(parentId)
                                .fullPath(sectionId + " " + title)
                                .tags(List.of())
                                .content(null)
                                .build();

                        buf.setLength(0);
                        continue;
                    }

                    // content appended if we have an active section
                    if (current != null && !IGNORE_CONTENT.matcher(line).find() && !isLikelyPageFurniture(line)) {
                        if (line.endsWith("-")) {
                            line = line.substring(0, line.length() - 1);
                        }
                        buf.append(line).append(' ');
                    }
                }
            }

            // finalize last
            if (current != null) {
                sections.add(finalizeSection(current, buf));
            }
        }

        log.info("Extracted {} sections from {}", sections.size(), pdfFile.getName());
        return sections;
    }

    /**
     * Heuristic: use printed page numbers only when they are mostly non-zero and
     * monotonic increasing (i.e., behave like real printed pages).
     */
    private static boolean shouldUsePrintedPages(int[] printed) {
        int n = printed.length - 1;
        if (n <= 2) return false;
        int nonZero = 0;
        int monotonic = 0;
        for (int p = 1; p <= n; p++) {
            if (printed[p] > 0) nonZero++;
            if (p > 1 && printed[p] > 0 && printed[p - 1] > 0 && printed[p] == printed[p - 1] + 1) {
                monotonic++;
            }
        }
        // require a minimum number of detected printed pages and reasonable monotonicity
        return nonZero >= Math.max(6, n / 10) && ((double) monotonic / Math.max(1, nonZero - 1) > 0.75);
    }

    private Section finalizeSection(Section head, StringBuilder buf) {
        String content = (buf == null) ? "" : buf.toString().trim();
        if (content.isEmpty()) {
            content = "[No extractable text — section may contain only figures/tables]";
        }

        return Section.builder()
                .docTitle(head.getDocTitle())
                .sectionId(head.getSectionId())
                .title(head.getTitle())
                .page(head.getPage())
                .level(head.getLevel())
                .parentId(head.getParentId())
                .fullPath(head.getFullPath())
                .tags(head.getTags() == null ? List.of() : head.getTags())
                .content(content)
                .build();
    }

    private static String normalize(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFC)
                .replace('\u00A0', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ");
        return n;
    }

    private static boolean isLikelyPageFurniture(String line) {
        if (line == null) return false;
        String l = line.toLowerCase().trim();
        if (l.isEmpty()) return true;
        if (l.contains("universal serial bus power delivery specification")) return true;
        if (l.matches("^page\\s+\\d+\\b.*")) return true;
        return false;
    }
}
