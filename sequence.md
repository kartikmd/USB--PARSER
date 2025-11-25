```mermaid
sequenceDiagram
    %% Participants
    actor Dev as Developer / Client
    participant App as UsbPdParserApplication
    participant Ctrl as PdfParserController<br/>(/api/pdf/parse)
    participant Storage as File System (output/)
    participant PdfBox as PDFBox Layer<br/>(Toc + Section Extractors)
    participant Writer as JsonlWriter (Jackson)
    participant Validator as ExcelValidator (Apache POI)
    participant Log as PerfLogger / Logback

    %% 1. Start + Upload
    Dev->>App: Start Spring Boot app
    Dev->>Ctrl: POST /api/pdf/parse (multipart PDF)
    Ctrl->>Ctrl: validate file (not null, not empty, is PDF)

    %% 2. Save PDF
    Ctrl->>Storage: save uploaded PDF under output/ (basePath)
    Storage-->>Ctrl: return saved PDF path
    Ctrl->>Log: log "Upload saved..." (time, CPU, memory)

    %% 3. Parse TOC with PDFBox
    Ctrl->>PdfBox: parse TOC (PdfBoxTocExtractor.parse(pdfFile))
    PdfBox-->>Ctrl: List<Section> tocSections
    Ctrl->>Log: log "ToC extracted..." (items, ms, CPU, memory)

    %% 4. Parse Sections with PDFBox
    Ctrl->>PdfBox: parse Sections (PdfBoxSectionExtractor.parse(pdfFile))
    PdfBox-->>Ctrl: List<Section> allSections
    Ctrl->>Log: log "Sections extracted..." (items, ms, CPU, memory)

    %% 5. Write JSONL (TOC + Sections)
    Ctrl->>Writer: write usb_pd_toc.jsonl (tocSections)
    Writer->>Storage: create/overwrite usb_pd_toc.jsonl
    Storage-->>Writer: file written

    Ctrl->>Writer: write usb_pd_sections.jsonl (allSections)
    Writer->>Storage: create/overwrite usb_pd_sections.jsonl
    Storage-->>Writer: file written
    Ctrl->>Log: log "JSONL written..." (total items, ms, CPU, memory)

    %% 6. Excel Validation
    Ctrl->>Validator: validate(tocSections, allSections)
    Validator->>Storage: create validation_report.xlsx
    Storage-->>Validator: report saved
    Validator-->>Ctrl: validation complete
    Ctrl->>Log: log "Validation report written..." (ms, CPU, memory)

    %% 7. Final summary + HTTP response
    Ctrl->>Log: log "Job complete..." (total time, CPU, memory)
    Ctrl-->>Dev: 200 OK + "Parsing complete. Results -> output/"

  Dev->>FS: download outputs (toc, sections, report)
