# System Architecture Diagram

```mermaid

   graph TD
    %% Ingestion
    subgraph Ingestion
        PDF["📄 Input PDF\n(USB_PD_R3_2 V1.1.pdf)\nInput: spec PDF file"]
        CLI["🖥️ CLI / Runner\nUsbParserRunner / UsbPdParserApplication\nInput: file path\nTrigger: parsing run"]
        API["🌐 Spring Boot API\nPdfParserController\nInput: upload / URL\nOutput: parse request"]
    end

    %% Parsing
    subgraph Parsing
        PdfBox["📦 PDF Parser (Apache PDFBox / PyMuPDF)\nTask: extract page text + layout\nOutput: raw text blocks & layout"]
        ToCExtractor["🧭 ToC Extractor\nPdfBoxTocExtractor\nTask: parse front-matter ToC lines\nOutput: usb_pd_toc.jsonl"]
        SectionExtractor["📑 Section Extractor\nPdfBoxSectionExtractor\nTask: split pages into sections\nOutput: raw section objects"]
        OCR["🔎 OCR Fallback (Tess4J / pytesseract)\nTask: OCR image-only pages\nOutput: OCR text appended to sections"]
    end

    %% Post-processing
    subgraph PostProcessing
        PostProc["🧼 SectionPostProcessor (optional)\nTask: clean headings, normalize titles, heuristics"]
        Dedup["🔁 Deduplicator\nTask: keep best Section per section_id"]
        JsonWriter["💾 JSONL Writer (Jackson)\nTask: write usb_pd_sections.jsonl"]
    end

    %% Validation & Reporting
    subgraph Validation
        Validator["✅ Validation Engine (ExcelValidator)\nTask: ToC ↔ Sections matching\nOutputs: validation_report.xlsx"]
        Rules["📋 Validation Rules\n- Missing / Extra sections\n- Page alignment\n- Table/figure counts"]
        ReportGen["📊 Excel Report Generator (Apache POI)\nTask: build validation workbook"]
    end

    %% Storage & Outputs
    subgraph StorageOutputs
        TOCFile["🗂 usb_pd_toc.jsonl"]
        SectionsFile["🗂 usb_pd_sections.jsonl"]
        ValidationFile["🗂 validation_report.xlsx"]
        Artifacts["📦 Archive (zip)\nPack: scripts + outputs + README"]
    end

    %% Observability & Utilities
    subgraph Observability
        Logger["📝 Logging (SLF4J + Logback)\nTask: record progress & errors"]
        Metrics["📈 Metrics & Perf\n(Execution time, pages processed)"]
        README["📚 README + Docs\nArchitecture, Usage, Notes"]
    end

    %% Connections / Flow
    CLI --> PDF
    API --> PDF
    PDF --> PdfBox
    PdfBox --> ToCExtractor
    PdfBox --> SectionExtractor
    SectionExtractor --> OCR
    OCR --> SectionExtractor
    SectionExtractor --> PostProc
    PostProc --> Dedup
    Dedup --> JsonWriter
    ToCExtractor --> TOCFile
    JsonWriter --> SectionsFile
    Dedup --> Validator
    ToCExtractor --> Validator
    Validator --> Rules
    Rules --> ReportGen
    ReportGen --> ValidationFile
    JsonWriter --> Artifacts
    TOCFile --> Artifacts
    ValidationFile --> Artifacts

    %% Observability links
    PdfBox --> Logger
    SectionExtractor --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> Metrics
    README --> Artifacts

