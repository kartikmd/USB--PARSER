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
        PDF["📄 Uploaded PDF\n(saved in output/)"]
    end

    %% Parsing
    subgraph Parsing
        PdfBox["📦 PDFBox Engine\n(PdfBoxTocExtractor, PdfBoxSectionExtractor)"]
        ToCExtractor["🧭 TOC Extractor\nExtracts TOC entries"]
        SectionExtractor["📑 Section Extractor\nExtracts all sections"]
    end

    %% Processing
    subgraph Processing
        PostProc["🧼 SectionPostProcessor\n(clean/normalize)"]
        Dedup["🔁 Deduplicator\n(chooseBetterSection)"]
        JsonWriter["💾 JsonlWriter (Jackson)\nWrites JSONL files"]
    end

    %% Validation
    subgraph Validation
        Validator["✅ ExcelValidator\n(Apache POI)"]
        ReportGen["📊 Report Generator\nCreates validation_report.xlsx"]
    end

    %% Logs & Performance
    subgraph Logging
        Logger["📝 SLF4J + Logback\nRecords every step"]
        Perf["📈 PerfLogger\nWrites to logs/performance.log"]
    end

    %% Outputs
    subgraph Outputs
        TOC["🗂 usb_pd_toc.jsonl"]
        SECS["🗂 usb_pd_sections.jsonl"]
        VALID["🗂 validation_report.xlsx"]
        PERFLOG["📄 performance.log\n(logs/performance.log)"]
    end

    %% Flow Connections
    Dev --> App
    App --> UploadAPI
    UploadAPI --> PDF

    PDF --> PdfBox
    PdfBox --> ToCExtractor
    PdfBox --> SectionExtractor

    %% TOC Flow
    ToCExtractor --> JsonWriter
    JsonWriter --> TOC

    %% Sections Flow
    SectionExtractor --> PostProc
    PostProc --> Dedup
    Dedup --> JsonWriter
    JsonWriter --> SECS

    %% Validation Flow
    ToCExtractor --> Validator
    JsonWriter --> Validator
    Validator --> ReportGen
    ReportGen --> VALID

    %% Logging at every step
    UploadAPI --> Logger
    PdfBox --> Logger
    ToCExtractor --> Logger
    SectionExtractor --> Logger
    JsonWriter --> Logger
    Validator --> Logger
    Logger --> Perf
    Perf --> PERFLOG

