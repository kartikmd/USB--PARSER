```mermaid
flowchart TB

    %% Validation
    subgraph Validation
        Validator["✅ ExcelValidator (Apache POI)\nCompares TOC.jsonl & Sections.jsonl"]
        ReportGen["📊 Report Generator\nCreates validation_report.xlsx"]
    end

    %% Observability
    subgraph Observability
        Logger["📝 SLF4J + Logback"]
        Perf["📈 PerfLogger\nWrites into logs/performance.log"]
    end

    %% Outputs
    subgraph Outputs
        TOC["🗂 usb_pd_toc.jsonl (output/)"]
        SECS["🗂 usb_pd_sections.jsonl (output/)"]
        VALID["🗂 validation_report.xlsx (output/)"]
        LOG["📄 performance.log (logs/)"]
    end

    %% Flows
    Dev["👨‍💻 Developer"] --> App["🌐 Spring Boot App"]
    App --> UploadAPI["📥 Upload API (/upload)"]

    UploadAPI --> PDF["📄 PDF File"]
    PDF --> PdfBox["📦 Apache PDFBox"]

    PdfBox --> ToCExtractor["🧾 PdfBoxTocExtractor"]
    PdfBox --> SectionExtractor["📑 PdfBoxSectionExtractor"]

    %% TOC flow
    ToCExtractor --> JsonWriter["💾 JSON Writer"]
    JsonWriter --> TOC

    %% Sections flow (no post-processing layers)
    SectionExtractor --> JsonWriter
    JsonWriter --> SECS

    %% Validation flow
    TOC --> Validator
    SECS --> Validator
    Validator --> ReportGen
    ReportGen --> VALID

    %% Logging
    UploadAPI --> Logger
    ToCExtractor --> Logger
    SectionExtractor --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> Perf
    Perf --> LOG
