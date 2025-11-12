graph TD

    %% Ingestion / Environment
    subgraph Environment
        UserPC["💻 Developer / Runner\nInput: PDF file path or upload\nOutput: Triggers parsing run"]
        CI["⚙️ CI / GitHub Actions\nInput: repo push\nOutput: runs tests & packaging"]
    end

    %% Software Components
    subgraph Software
        CLI["🖥️ CLI Runner\nUsbParserRunner / main()\nInput: PDF path\nTask: Orchestrate parse -> postprocess -> write"]
        API["🌐 Spring Boot API\nPdfParserController (optional)\nInput: upload / URL\nTask: expose parse endpoint"]
        PdfBox["📦 PDF Parser (PdfBoxSectionExtractor)\nInput: PDF bytes\nTask: extract text blocks & layout"]
        ToC["🧭 ToC Extractor (PdfBoxTocExtractor)\nInput: front-matter text\nTask: parse ToC lines -> section_ids"]
        SectionExt["📑 Section Extractor\nInput: page text & headings\nTask: split into Section objects"]
        OCR["🔎 OCR Fallback (Tess4J/pytesseract)\nInput: image-only pages\nTask: recover text, mark source=\"ocr\""]
        PostProc["🧼 SectionPostProcessor\nInput: raw Sections\nTask: clean titles, normalize, heuristics"]
        Dedup["🔁 Deduplicator\nInput: Sections\nTask: chooseBest per section_id"]
        JsonWriter["💾 JsonlWriter (Jackson)\nInput: Sections\nTask: write usb_pd_sections.jsonl"]
        ToCWriter["🗂 ToC Writer\nTask: write usb_pd_toc.jsonl"]
        Validator["✅ ExcelValidator (Apache POI)\nInput: ToC + Sections\nTask: validate, produce validation_report.xlsx"]
        ReportGen["📊 Report Generator\nTask: build validation_report.xlsx"]
    end

    %% Observability / Utilities
    subgraph Observability
        Logger["📝 Logging (SLF4J + Logback)\nInput: events & errors\nTask: write to performance.log"]
        Perf["📈 PerfProbe / PerfLogger\nTask: track time, pages/sec"]
        README["📚 README & Docs\nTask: usage, architecture, notes"]
    end

    %% Storage / Artifacts
    subgraph Storage
        TOCFile["🗂 usb_pd_toc.jsonl\nContains: section_id, title, page, level, parent_id"]
        SectionsFile["🗂 usb_pd_sections.jsonl\nContains: Section objects (content, page, full_path)"]
        ValidationFile["🗂 validation_report.xlsx\nContains: ToC vs Sections validation"]
        Logs["📄 performance.log\nContains: execution metrics, errors"]
        RepoZip["📦 release.zip / repo\nContains: code + outputs + README"]
    end

    %% Connections / Flow
    UserPC --> CLI
    UserPC --> API
    CLI --> PdfBox
    API --> PdfBox
    PdfBox --> ToC
    PdfBox --> SectionExt
    SectionExt --> OCR
    OCR --> SectionExt
    SectionExt --> PostProc
    PostProc --> Dedup
    Dedup --> JsonWriter
    ToC --> ToCWriter
    ToCWriter --> TOCFile
    JsonWriter --> SectionsFile
    ToCWriter --> Validator
    JsonWriter --> Validator
    Validator --> ReportGen
    ReportGen --> ValidationFile

    %% Observability links
    PdfBox --> Logger
    SectionExt --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> Logs
    Logger --> Perf
    README --> RepoZip
    TOCFile --> RepoZip
    SectionsFile --> RepoZip
    ValidationFile --> RepoZip
