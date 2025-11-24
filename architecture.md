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

    %% Injection / Parsing Layer
    subgraph InjectionLayer["🧩 Injection / Parsing Layer"]
        Normalizer["🔤 TextNormalizer\n(normalize(), remove NBSP, unicode)"]
        PreJoin["🔗 PreJoin / Merge\n(page-only merge, dotted leaders)"]
        RegexEngine["🔎 RegexEngine\n(common regex patterns)"]
        TableRepair["🛠 TableRepair\nnumeric-key fixes & post-repair"]
        DI["⚙️ DI Container\n(provide same instance to extractors)"]
    end

    %% Extractors & PdfBox
    PdfBox["📦 Apache PDFBox"]
    ToCExtractor["🧾 PdfBoxTocExtractor"]
    SectionExtractor["📑 PdfBoxSectionExtractor"]
    JsonWriter["💾 JsonWriter"]

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
    PDF --> PdfBox

    %% Injection: PdfBox -> Injection Layer -> Extractors
    PdfBox --> DI
    DI --> Normalizer
    DI --> PreJoin
    DI --> RegexEngine
    DI --> TableRepair

    PdfBox --> InjectionLayer
    InjectionLayer --> ToCExtractor
    InjectionLayer --> SectionExtractor

    %% Extractors -> Writer -> Outputs
    ToCExtractor --> JsonWriter
    JsonWriter --> TOC

    SectionExtractor --> JsonWriter
    JsonWriter --> SECS

    %% Validation flow
    TOC --> Validator
    SECS --> Validator
    Validator --> ReportGen
    ReportGen --> VALID

    %% Logging
    UploadAPI --> Logger
    PdfBox --> Logger
    InjectionLayer --> Logger
    ToCExtractor --> Logger
    SectionExtractor --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> Perf
    Perf --> LOG
