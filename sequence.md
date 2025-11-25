```mermaid
sequenceDiagram
    %% Participants
    actor Dev as Developer / Client
    participant App as Spring Boot App
    participant Ctrl as PdfParserController
    participant Storage as File System (output/)
    participant PdfBox as PDFBox Extractors<br/>(TOC + Sections)
    participant Writer as JsonlWriter (Jackson)
    participant Validator as ExcelValidator (Apache POI)
    participant Log as PerfLogger / Logback

    %% ENTRY + UPLOAD
    Dev->>App: Starts Application
    Dev->>Ctrl: POST /api/pdf/parse (Upload PDF)
    Ctrl->>Ctrl: Validate file (PDF check)
    Ctrl->>Storage: Save PDF inside output/
    Storage-->>Ctrl: File stored path
    Ctrl->>Log: "Upload saved" (time, CPU, memory)

    %% PARSING PHASE
    Ctrl->>PdfBox: Extract TOC (PdfBoxTocExtractor)
    PdfBox-->>Ctrl: tocSections (List<Section>)
    Ctrl->>Log: "TOC extracted" (count, time)

    Ctrl->>PdfBox: Extract Sections (PdfBoxSectionExtractor)
    PdfBox-->>Ctrl: allSections (List<Section>)
    Ctrl->>Log: "Sections extracted" (count, time)

    %% WRITE JSONL OUTPUTS
    Ctrl->>Writer: Write usb_pd_toc.jsonl
    Writer->>Storage: Save TOC JSONL
    Storage-->>Writer: File written

    Ctrl->>Writer: Write usb_pd_sections.jsonl
    Writer->>Storage: Save Sections JSONL
    Storage-->>Writer: File written
    Ctrl->>Log: "JSONL written" (rows, time)

    %% VALIDATION PHASE
    Ctrl->>Validator: Compare TOC vs Sections
    Validator->>Storage: Generate validation_report.xlsx
    Storage-->>Validator: File saved
    Validator-->>Ctrl: Validation complete
    Ctrl->>Log: "Validation report written"

    %% FINAL RESPONSE
    Ctrl->>Log: "Job complete" (total runtime, memory)
    Ctrl-->>Dev: 200 OK - Results available in output/

  Dev->>FS: download outputs (toc, sections, report)
