package com.myorg.usbparser.controller;

import com.myorg.usbparser.config.StorageProperties;
import com.myorg.usbparser.exception.ValidationException;
import com.myorg.usbparser.model.Section;
import com.myorg.usbparser.service.JsonlWriter;
import com.myorg.usbparser.service.SectionExtractor;
import com.myorg.usbparser.service.TocExtractor;
import com.myorg.usbparser.service.Validator;
import com.myorg.usbparser.service.implementation.ExcelValidator;
import com.myorg.usbparser.service.implementation.JacksonJsonlWriter;
import com.myorg.usbparser.service.implementation.PdfBoxSectionExtractor;
import com.myorg.usbparser.service.implementation.PdfBoxTocExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pdf")
public class PdfParserController {

    private static final String DOC_TITLE = "USB Power Delivery Specification Rev 3.2";
    private final StorageProperties storageProperties;

    private static final Logger PerfLogger = LoggerFactory.getLogger("performance");

    @PostMapping("/parse")
    public ResponseEntity<String> parsePdf(@RequestParam("file") MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new ValidationException("Please upload a non-empty PDF file.");
        }

        final String originalName = file.getOriginalFilename();
        if (originalName == null) {
            throw new ValidationException("Uploaded file has no filename.");
        }

        final String lower = originalName.toLowerCase();
        final String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();

        if (!(lower.endsWith(".pdf") || contentType.contains("pdf"))) {
            throw new ValidationException("Only PDF files are accepted.");
        }

        long jobStart = System.nanoTime();

        try {
            Path outDir = Path.of(storageProperties.getBasePath()).toAbsolutePath().normalize();
            Files.createDirectories(outDir);

            // Save upload
            Path pdfPath = outDir.resolve(Path.of(originalName).getFileName().toString());
            long t0 = System.nanoTime();
            Files.copy(file.getInputStream(), pdfPath, StandardCopyOption.REPLACE_EXISTING);
            perf("Upload saved", t0, 1);

            PerfLogger.info("Parsing started: {}", originalName);

            // ToC extraction
            t0 = System.nanoTime();
            TocExtractor tocExtractor = new PdfBoxTocExtractor(DOC_TITLE);
            List<Section> tocSections = tocExtractor.parse(pdfPath.toFile());
            perf("ToC extracted", t0, tocSections.size());

            // Sections extraction
            t0 = System.nanoTime();
            SectionExtractor sectionExtractor = new PdfBoxSectionExtractor(DOC_TITLE);
            List<Section> allSections = sectionExtractor.parse(pdfPath.toFile());
            perf("Sections extracted", t0, allSections.size());

            // JSONL writing
            JsonlWriter<Section> writer = new JacksonJsonlWriter<>();
            t0 = System.nanoTime();
            writer.write(outDir.resolve("usb_pd_toc.jsonl").toFile(), tocSections);
            writer.write(outDir.resolve("usb_pd_sections.jsonl").toFile(), allSections);
            perf("JSONL written", t0, tocSections.size() + allSections.size());

            // Excel Validation
            t0 = System.nanoTime();
            Validator validator = new ExcelValidator(outDir.resolve("validation_report.xlsx").toFile());
            validator.validate(tocSections, allSections);
            perf("Validation report written", t0, 1);

            // Job total
            long totalMs = msSince(jobStart);
            PerfLogger.info(
                    "Job complete: {} ms, CPU: {}%, Memory: {} MB",
                    totalMs,
                    format(cpuLoad()),
                    format(mb(usedMemoryBytes()))
            );

            return ResponseEntity.ok("Parsing complete. Results → " + outDir);

        } catch (Exception ex) {
            log.error("Parsing failed for {}: {}", originalName, ex.getMessage(), ex);
            throw new ValidationException("Failed to parse uploaded PDF.", ex);
        }
    }

    @GetMapping("/results/toc")
    public ResponseEntity<FileSystemResource> getTocJsonl() {
        return serveFile("usb_pd_toc.jsonl");
    }

    @GetMapping("/results/sections")
    public ResponseEntity<FileSystemResource> getSectionsJsonl() {
        return serveFile("usb_pd_sections.jsonl");
    }

    @GetMapping("/results/validation")
    public ResponseEntity<FileSystemResource> getValidationReport() {
        return serveFile("validation_report.xlsx");
    }

    // ===== Helpers =====

    private ResponseEntity<FileSystemResource> serveFile(String name) {
        Path outDir = Path.of(storageProperties.getBasePath()).toAbsolutePath().normalize();
        File f = outDir.resolve(name).toFile();

        if (!f.exists()) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + name + "\"")
                .body(new FileSystemResource(f));
    }

    private void perf(String step, long startNano, int items) {
        long ms = msSince(startNano);
        double rate = items > 0 ? items / (ms / 1000.0) : 0;

        PerfLogger.info(
                "{}: {} ms (Rate/FPS: {}), CPU: {}%, Memory: {} MB, items={}",
                step,
                ms,
                format(rate),
                format(cpuLoad()),
                format(mb(usedMemoryBytes())),
                items
        );
    }

    private static long msSince(long nano) {
        return Duration.ofNanos(System.nanoTime() - nano).toMillis();
    }

    private static long usedMemoryBytes() {
        MemoryMXBean bean = ManagementFactory.getMemoryMXBean();
        long used = bean.getHeapMemoryUsage().getUsed();
        MemoryUsage non = bean.getNonHeapMemoryUsage();
        if (non != null) used += Math.max(0, non.getUsed());
        return used;
    }

    private static double cpuLoad() {
        try {
            var os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean m) {
                double val = m.getProcessCpuLoad();
                return val < 0 ? -1 : val * 100;
            }
        } catch (Exception ignored) {}
        return -1;
    }

    private static double mb(long bytes) {
        return bytes / (1024.0 * 1024.0);
    }

    private static String format(double v) {
        return v < 0 ? "n/a" : String.format("%.2f", v);
    }
}
