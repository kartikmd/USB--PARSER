```mermaid
sequenceDiagram
    %% Participants
    actor Dev as Developer / Client
    participant App as UsbPdParserApplication
    participant Ctrl as PdfParserController
    participant Storage as File System (output/)
    participant PdfBox as PDFBox Layer<br/>(Toc + Section Extractors)
    participant Writer as JsonlWriter (Jackson)
    participant Validator as ExcelValidator (Apache POI)
    participant Log as PerfLogger / Logback

    %% 1. Entry + Ingestion
    Dev->>App: Start Spring Boot app
    Dev->>Ctrl: POST /api/pdf/parse (multipart PDF)
    Ctrl->>Ctrl: validate file (non-empty, is PDF)
    Ctrl->>Storage: save PDF under output/
    Storage-->>Ctrl: PDF path
    Ctrl->>Log: log "Upload saved..." (time, CPU, memory)

    %% 2. Parsing (TOC + Sections)
    Ctrl->>PdfBox: parse TOC (PdfBoxTocExtractor)
    PdfBox-->>Ctrl: List<Section> tocSections
    Ctrl->>Log: log "ToC extracted..." (items, ms)

    Ctrl->>PdfBox: parse Sections (PdfBoxSectionExtractor)
    PdfBox-->>Ctrl: List<Section> allSections
    Ctrl->>Log: log "Sections extracted..." (items, ms)

    %% 3. JSONL Writing (no post-processing/dedup)
    Ctrl->>Writer: write usb_pd_toc.jsonl (tocSections)
    Writer->>Storage: create/overwrite usb_pd_toc.jsonl
    Storage-->>Writer: file written

    Ctrl->>Writer: write usb_pd_sections.jsonl (allSections)
    Writer->>Storage: create/overwrite usb_pd_sections.jsonl
    Storage-->>Writer: file written
    Ctrl->>Log: log "JSONL written..." (total items, ms)

    %% 4. Validation (Excel)
    Ctrl->>Validator: validate(tocSections, allSections)
    Validator->>Storage: create validation_report.xlsx
    Storage-->>Validator: report saved
    Validator-->>Ctrl: validation done
    Ctrl->>Log: log "Validation report written..." (ms)

    %% 5. Final summary + HTTP response
    Ctrl->>Log: log "Job complete..." (total ms, CPU, memory)
    Ctrl-->>Dev: 200 OK + "Parsing complete. Results -> output/"
 entry
  Dev->>FS: download outputs (toc, sections, report)
