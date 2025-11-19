```mermaid
graph TD

    %% Entry
    subgraph Entry
        Dev["👤 Developer"]
        App["🌐 Spring Boot App\nUsbPdParserApplication"]
    end

    %% Ingestion
    subgraph Ingestion
        UploadAPI["📤 PdfParserController (REST API)\nAccepts PDF upload"]
        PDF["📄 Input PDF\n(saved to output/)"]
    end

    %% Parsing
    subgraph Parsing
        PdfBox["📦 PDFBox Layer\n(PdfBoxTocExtractor + PdfBoxSectionExtractor)"]
        ToCExtractor["🧭 TOC Extractor\n-> usb_pd_toc.jsonl (output/)"]
        SectionExtractor["📑 Section Extractor\n-> Section POJOs"]
    end

    %% Processing
    subgraph Processing
        PostProc["🧼 SectionPostProcessor\n(clean + merge)"]
        Dedup["🔁 Deduplicator\n(chooseBetterSection)"]
        JsonWriter["💾 JsonlWriter (Jackson)\n-> usb_pd_sections.jsonl"]
    end

    %% Validation
    subgraph Validation
        Validator["✅ ExcelValidator\n(uses Apache POI)"]
        ReportGen["📊 Report Generator\n-> validation_report.xlsx"]
    end

    %% Observability
    subgraph Observability
        Logger["📝 SLF4J + Logback\nwrites performance logs"]
        Perf["📈 PerfProbe\n-> logs/performance.log"]
    end

    %% Outputs
    subgraph Outputs
        TOC["🗂 usb_pd_toc.jsonl"]
        SECS["🗂 usb_pd_sections.jsonl"]
        VALID["🗂 validation_report.xlsx"]
        LOG["📄 performance.log (logs/)"]
    end

    %% Flows
    Dev --> App
    App --> UploadAPI
    UploadAPI --> PDF

    PDF --> PdfBox
    PdfBox --> ToCExtractor
    PdfBox --> SectionExtractor

    SectionExtractor --> PostProc
    PostProc --> Dedup
    Dedup --> JsonWriter

    ToCExtractor --> TOC
    JsonWriter --> SECS

    ToCExtractor --> Validator
    JsonWriter --> Validator
    Validator --> ReportGen
    ReportGen --> VALID

    %% Logging at every step
    UploadAPI --> Logger
    ToCExtractor --> Logger
    SectionExtractor --> Logger
    JsonWriter --> Logger
    Validator --> Logger
    ReportGen --> Logger
    Logger --> LOG
    Logger --> Perf
