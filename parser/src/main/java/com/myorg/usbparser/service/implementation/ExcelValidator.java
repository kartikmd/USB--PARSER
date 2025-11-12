package com.myorg.usbparser.service.implementation;

import com.myorg.usbparser.exception.ValidationException;
import com.myorg.usbparser.model.Section;
import com.myorg.usbparser.model.ValidationResult;
import com.myorg.usbparser.service.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ExcelValidator implements Validator {
    private static final String SUMMARY_SHEET = "Summary";
    private static final String MISSING_SHEET = "Missing Sections";
    private static final String EXTRA_SHEET = "Extra Sections";
    private static final String TABLES_SHEET = "Table Counts";
    private final File outputFile;

    public ExcelValidator(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public ValidationResult validate(List<Section> tocSections, List<Section> parsedSections) {
        if (tocSections == null) tocSections = List.of();
        if (parsedSections == null) parsedSections = List.of();
        try {
            ValidationResult result = computeValidationResult(tocSections, parsedSections);
            if (outputFile == null) {
                log.warn("outputFile is null — skipping writing Excel report");
                return result;
            }
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                log.warn("Could not create parent directories for: {}", parent.getAbsolutePath());
            }
            writeExcel(result);
            Map<String, Integer> tableCounts = result.getTableCounts();
            log.info("✅ Validation complete → TOC={}, Parsed={}, Missing={}, Extra={}, Tables(TOC/Parsed)={}/{}",
                    result.getTocSectionCount(),
                    result.getParsedSectionCount(),
                    result.getMissingCount(),
                    result.getExtraCount(),
                    tableCounts.getOrDefault("toc_tables_total", 0),
                    tableCounts.getOrDefault("parsed_tables_total", 0));
            return result;
        } catch (IOException e) {
            throw new ValidationException("Validation failed", e);
        }
    }

    private ValidationResult computeValidationResult(List<Section> tocSections, List<Section> parsedSections) {
        Function<Section, String> keyFn = s -> {
            String id = s == null ? "" : (s.getSectionId() == null ? "" : s.getSectionId()).trim();
            String titleNorm = normalizeForKey(s == null ? null : s.getTitle());
            return (id + " " + titleNorm).trim().toLowerCase();
        };

        LinkedHashMap<String, Section> tocMap = new LinkedHashMap<>();
        for (Section s : tocSections) tocMap.putIfAbsent(keyFn.apply(s), s);

        LinkedHashMap<String, Section> parsedMap = new LinkedHashMap<>();
        for (Section s : parsedSections) parsedMap.putIfAbsent(keyFn.apply(s), s);

        List<String> missingKeys = tocMap.keySet().stream()
                .filter(k -> !parsedMap.containsKey(k))
                .sorted()
                .collect(Collectors.toList());

        List<String> extraKeys = parsedMap.keySet().stream()
                .filter(k -> !tocMap.containsKey(k))
                .sorted()
                .collect(Collectors.toList());

        List<Section> missingSections = missingKeys.stream()
                .map(tocMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<Section> extraSections = extraKeys.stream()
                .map(parsedMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int tocTables = (int) tocSections.stream()
                .map(Section::getTitle)
                .filter(Objects::nonNull)
                .filter(t -> t.trim().toLowerCase().startsWith("table"))
                .count();

        int parsedTables = (int) parsedSections.stream()
                .map(Section::getTitle)
                .filter(Objects::nonNull)
                .filter(t -> t.trim().toLowerCase().startsWith("table"))
                .count();

        Map<String, Integer> tableCounts = new HashMap<>();
        tableCounts.put("toc_tables_total", tocTables);
        tableCounts.put("parsed_tables_total", parsedTables);

        return ValidationResult.builder()
                .tocSectionCount(tocMap.size())
                .parsedSectionCount(parsedMap.size())
                .missingSections(missingSections.stream().map(this::makeReportKey).collect(Collectors.toList()))
                .extraSections(extraSections.stream().map(this::makeReportKey).collect(Collectors.toList()))
                .missingCount(missingSections.size())
                .extraCount(extraSections.size())
                .tableCounts(tableCounts)
                .build();
    }

    private String makeReportKey(Section s) {
        String id = (s == null || s.getSectionId() == null) ? "" : s.getSectionId();
        String title = (s == null || s.getTitle() == null) ? "" : s.getTitle();
        String page = (s == null || s.getPage() == null) ? "" : String.valueOf(s.getPage());
        return String.format("%s | %s | page=%s", id, shorten(title, 200), page);
    }

    private String shorten(String s, int max) {
        if (s == null) return "";
        return (s.length() <= max) ? s : s.substring(0, max - 3) + "...";
    }

    private String normalizeForKey(String raw) {
        if (raw == null) return "";
        String n = raw.replaceAll("\\.{2,}", " ")
                .replace('\u00A0', ' ')
                .replaceAll("[^\\p{Alnum}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
        n = n.replaceAll("\\s+(19|20)\\d{2}$", "").trim();
        return n;
    }

    private void writeExcel(ValidationResult result) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle header = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            header.setFont(bold);

            Sheet sSummary = workbook.createSheet(SUMMARY_SHEET);
            addRow(sSummary, 0, "Metric", "Value", header);
            addRow(sSummary, 1, "TOC Sections", result.getTocSectionCount(), null);
            addRow(sSummary, 2, "Parsed Sections", result.getParsedSectionCount(), null);
            addRow(sSummary, 3, "Missing Sections", result.getMissingCount(), null);
            addRow(sSummary, 4, "Extra Sections", result.getExtraCount(), null);
            addRow(sSummary, 5, "Tables (TOC total)", val(result.getTableCounts(), "toc_tables_total"), null);
            addRow(sSummary, 6, "Tables (Parsed total)", val(result.getTableCounts(), "parsed_tables_total"), null);

            List<String> missingSample = result.getMissingSections().subList(0,
                    Math.min(5, result.getMissingSections().size()));
            List<String> extraSample = result.getExtraSections().subList(0,
                    Math.min(5, result.getExtraSections().size()));
            addRow(sSummary, 8, "Missing sample (top 5)", String.join(" || ", missingSample), null);
            addRow(sSummary, 9, "Extra sample (top 5)", String.join(" || ", extraSample), null);

            Sheet sMissing = workbook.createSheet(MISSING_SHEET);
            addRow(sMissing, 0, "section_id", "title | page | full_path", header);
            List<String> missing = result.getMissingSections();
            for (int i = 0; i < (missing == null ? 0 : missing.size()); i++) {
                Row r = sMissing.createRow(i + 1);
                r.createCell(0).setCellValue(extractColumn(missing.get(i), 0));
                r.createCell(1).setCellValue(extractColumn(missing.get(i), 1));
            }

            Sheet sExtra = workbook.createSheet(EXTRA_SHEET);
            addRow(sExtra, 0, "section_id", "title | page | full_path", header);
            List<String> extra = result.getExtraSections();
            for (int i = 0; i < (extra == null ? 0 : extra.size()); i++) {
                Row r = sExtra.createRow(i + 1);
                r.createCell(0).setCellValue(extractColumn(extra.get(i), 0));
                r.createCell(1).setCellValue(extractColumn(extra.get(i), 1));
            }

            Sheet sTables = workbook.createSheet(TABLES_SHEET);
            addRow(sTables, 0, "Source", "Tables Total", header);
            addRow(sTables, 1, "ToC", val(result.getTableCounts(), "toc_tables_total"), null);
            addRow(sTables, 2, "Parsed", val(result.getTableCounts(), "parsed_tables_total"), null);

            int missingRows = (missing == null) ? 0 : missing.size();
            int extraRows = (extra == null) ? 0 : extra.size();
            if (missingRows + extraRows < 2000) {
                autoSize(sMissing, 2);
                autoSize(sExtra, 2);
            } else {
                sMissing.setColumnWidth(0, 8000);
                sMissing.setColumnWidth(1, 20000);
                sExtra.setColumnWidth(0, 8000);
                sExtra.setColumnWidth(1, 20000);
            }
            autoSize(sSummary, 2);
            autoSize(sTables, 2);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }

            log.info("✅ Validation report written to {}", outputFile.getAbsolutePath());
            log.info("Missing sample: {}", missingSample);
            log.info("Extra sample: {}", extraSample);
        }
    }

    private String extractColumn(String reportLine, int col) {
        if (reportLine == null) return "";
        String[] parts = reportLine.split("\\|", 3);
        for (int i = 0; i < parts.length; i++) parts[i] = parts[i].trim();
        if (col < parts.length) return parts[col];
        return "";
    }

    private void addRow(Sheet sheet, int rowIndex, String key, Object value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        Cell k = row.createCell(0);
        k.setCellValue(key == null ? "" : key);
        if (style != null) k.setCellStyle(style);
        if (value != null) {
            Cell v = row.createCell(1);
            v.setCellValue(String.valueOf(value));
            if (style != null) v.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int c = 0; c < cols; c++) sheet.autoSizeColumn(c);
    }

    private int val(Map<String, Integer> m, String k) {
        return (m == null || !m.containsKey(k) || m.get(k) == null) ? 0 : m.get(k);
    }
}
