package com.myorg.usbparser.controller;

import com.myorg.usbparser.model.Section;
import com.myorg.usbparser.service.JsonlWriter;
import com.myorg.usbparser.service.SectionExtractor;
import com.myorg.usbparser.service.TocExtractor;
import com.myorg.usbparser.service.Validator;
import com.myorg.usbparser.service.implementation.ExcelValidator;
import com.myorg.usbparser.service.implementation.JacksonJsonlWriter;
import com.myorg.usbparser.service.implementation.PdfBoxSectionExtractor;
import com.myorg.usbparser.service.implementation.PdfBoxTocExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pdf")
public class PdfParserController {

    private final String docTitle = "USB Power Delivery Specification Rev 3.2";
    private final File outputDir = new File(System.getProperty("user.dir"), "output");

    @PostMapping("/parse")
    public ResponseEntity<String> parsePdf(@RequestParam("file") MultipartFile file) {
        // Basic input validation
        if (file == null || file.isEmpty()) {
            log.warn("No file provided or file is empty in upload.");
            return ResponseEntity.badRequest().body("Please upload a non-empty PDF file.");
        }

        // sanitize filename to avoid path traversal
        String original = file.getOriginalFilename();
        if (original == null) {
            log.warn("Uploaded file has no original filename.");
            return ResponseEntity.badRequest().body("Uploaded file has no filename.");
        }
        String safeName = Paths.get(original).getFileName().toString();

        try {
            // ensure output directory exists
            Path outPath = outputDir.toPath();
            if (!Files.exists(outPath)) {
                Files.createDirectories(outPath);
                log.info("Created output directory: {}", outPath.toAbsolutePath());
            }

            // Save uploaded PDF inside output/
            File pdfFile = outPath.resolve(safeName).toFile();
            file.transferTo(pdfFile);

            // Step 1: Extract ToC
            TocExtractor tocExtractor = new PdfBoxTocExtractor(docTitle);
            List<Section> tocSections = tocExtractor.parse(pdfFile);

            // Step 2: Extract all sections
            SectionExtractor sectionExtractor = new PdfBoxSectionExtractor(docTitle);
            List<Section> allSections = sectionExtractor.parse(pdfFile);

            // Step 3: Write outputs
            JsonlWriter<Section> writer = new JacksonJsonlWriter<>();
            writer.write(new File(outputDir, "usb_pd_toc.jsonl"), tocSections);
            writer.write(new File(outputDir, "usb_pd_sections.jsonl"), allSections);

            // Step 4: Validation
            File validationFile = new File(outputDir, "validation_report.xlsx");
            Validator validator = new ExcelValidator(validationFile);
            validator.validate(tocSections, allSections);

            log.info("Parsing complete for file: {}. Results saved in: {}", safeName, outputDir.getAbsolutePath());
            return ResponseEntity.ok("✅ Parsing complete. Results saved in: " + outputDir.getAbsolutePath());

        } catch (Exception ex) {
            // Let GlobalExceptionHandler handle detailed response; log summary here.
            log.error("Error during PDF parsing for file {}: {}", safeName, ex.getMessage(), ex);
            throw new RuntimeException("Failed to parse uploaded PDF", ex);
        }
    }

    @GetMapping("/results/toc")
    public ResponseEntity<FileSystemResource> getTocJsonl() {
        File tocFile = new File(outputDir, "usb_pd_toc.jsonl");
        if (!tocFile.exists()) return ResponseEntity.notFound().build();
        FileSystemResource resource = new FileSystemResource(tocFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"usb_pd_toc.jsonl\"")
                .body(resource);
    }

    @GetMapping("/results/sections")
    public ResponseEntity<FileSystemResource> getSectionsJsonl() {
        File secFile = new File(outputDir, "usb_pd_sections.jsonl");
        if (!secFile.exists()) return ResponseEntity.notFound().build();
        FileSystemResource resource = new FileSystemResource(secFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"usb_pd_sections.jsonl\"")
                .body(resource);
    }

    @GetMapping("/results/validation")
    public ResponseEntity<FileSystemResource> getValidationReport() {
        File valFile = new File(outputDir, "validation_report.xlsx");
        if (!valFile.exists()) return ResponseEntity.notFound().build();
        FileSystemResource resource = new FileSystemResource(valFile);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"validation_report.xlsx\"")
                .body(resource);
    }
}
