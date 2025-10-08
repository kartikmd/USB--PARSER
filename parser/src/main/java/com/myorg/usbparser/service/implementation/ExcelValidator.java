package com.myorg.usbparser.service.implementation;

import com.myorg.usbparser.model.Section;
import com.myorg.usbparser.model.ValidationResult;
import com.myorg.usbparser.service.Validator;
import com.myorg.usbparser.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Validates TOC vs parsed sections and writes an Excel validation report.
 */
@Slf4j
public class ExcelValidator implements Validator {

    private static final String SUMMARY_SHEET = "Summary";
    private static final String MISSING_SHEET = "Missing Sections";
    private static final String EXTRA_SHEET = "Extra Sections";

    private final File outputFile;

    public ExcelValidator(File outputFile) {
        this.outputFile = outputFile;
    }

    @Override
    public ValidationResult validate(List<Section> tocSections, List<Section> parsedSections) {
        try {
            Set<String> tocKeys = tocSections.stream()
                    .map(s -> (s.getSectionId() != null ? s.getSectionId() : "") + " " + normalize(s.getTitle()))
                    .collect(Collectors.toSet());

            Set<String> parsedKeys = parsedSections.stream()
                    .map(s -> (s.getSectionId() != null ? s.getSectionId() : "") + " " + normalize(s.getTitle()))
                    .collect(Collectors.toSet());

            List<String> missing = tocKeys.stream()
                    .filter(id -> !parsedKeys.contains(id))
                    .sorted()
                    .collect(Collectors.toList());

            List<String> extra = parsedKeys.stream()
                    .filter(id -> !tocKeys.contains(id))
                    .sorted()
                    .collect(Collectors.toList());

            ValidationResult result = ValidationResult.builder()
                    .tocSectionCount(tocKeys.size())
                    .parsedSectionCount(parsedKeys.size())
                    .missingSections(missing)
                    .extraSections(extra)
                    .missingCount(missing.size())
                    .extraCount(extra.size())
                    .tableCounts(new HashMap<>())
                    .build();

            writeExcel(result);

            log.info("Validation complete → TOC={}, Parsed={}, Missing={}, Extra={}",
                    result.getTocSectionCount(),
                    result.getParsedSectionCount(),
                    result.getMissingCount(),
                    result.getExtraCount());

            return result;
        } catch (IOException e) {
            throw new ValidationException("Validation failed", e);
        }
    }

    private String normalize(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("\\.{2,}", " ")
                .replaceAll("\\s+\\d+$", "")
                .trim()
                .toLowerCase();
    }

    /**
     * Write validation results to an Excel file. Uses try-with-resources to ensure workbook/stream are closed.
     */
    private void writeExcel(ValidationResult result) throws IOException {
        // Ensure parent directory exists
        if (outputFile != null) {
            File parent = outputFile.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean ok = parent.mkdirs();
                if (!ok) {
                    log.warn("Could not create parent directories for output file: {}", parent.getAbsolutePath());
                }
            }
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            // Create bold header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);

            // Sheet 1: Summary
            Sheet summarySheet = workbook.createSheet(SUMMARY_SHEET);
            addRow(summarySheet, 0, "Metric", "Value", headerStyle);
            addRow(summarySheet, 1, "TOC Sections", result.getTocSectionCount(), null);
            addRow(summarySheet, 2, "Parsed Sections", result.getParsedSectionCount(), null);
            addRow(summarySheet, 3, "Missing Sections", result.getMissingCount(), null);
            addRow(summarySheet, 4, "Extra Sections", result.getExtraCount(), null);
            summarySheet.autoSizeColumn(0);
            summarySheet.autoSizeColumn(1);

            // Sheet 2: Missing Sections
            Sheet missingSheet = workbook.createSheet(MISSING_SHEET);
            List<String> missing = result.getMissingSections();
            for (int i = 0; i < (missing == null ? 0 : missing.size()); i++) {
                missingSheet.createRow(i).createCell(0).setCellValue(missing.get(i));
            }
            missingSheet.autoSizeColumn(0);

            // Sheet 3: Extra Sections
            Sheet extraSheet = workbook.createSheet(EXTRA_SHEET);
            List<String> extra = result.getExtraSections();
            for (int i = 0; i < (extra == null ? 0 : extra.size()); i++) {
                extraSheet.createRow(i).createCell(0).setCellValue(extra.get(i));
            }
            extraSheet.autoSizeColumn(0);

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                workbook.write(fos);
            }

            log.info("Validation report written to {}", outputFile == null ? "stdout" : outputFile.getAbsolutePath());
        }
    }

    private void addRow(Sheet sheet, int rowIndex, String key, Object value, CellStyle style) {
        Row row = sheet.createRow(rowIndex);
        Cell keyCell = row.createCell(0);
        Cell valueCell = row.createCell(1);

        keyCell.setCellValue(key == null ? "" : key);
        valueCell.setCellValue(value == null ? "" : String.valueOf(value));

        if (style != null) {
            keyCell.setCellStyle(style);
            valueCell.setCellStyle(style);
        }
    }
}
