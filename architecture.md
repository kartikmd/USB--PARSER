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
        PDF["📄 Input PDF\nUSB_PD_R3_2.pdf (saved under output/uploads/)"]
    end

    %% Parsing
    subgraph Parsing
        PdfBox["📦 PDFBox Layer\n(PdfBoxTocExtractor, PdfBoxSectionExtractor)"]
        ToCExtractor["🧭 ToC Extractor\n-> tocSections (in-memory)"]
        SectionExtractor["📑 Section Extractor\n-> allSections (in-memory)"]
    end

    %% Processing
    subgraph Processing
        PostProc["🧼 SectionPostProcessor\n(normalize / merge)"]
        Dedup["🔁 Deduplicator\n(chooseBetterSection)"]
        JsonWriter["💾 JsonlWriter (Jackson)\nWrites usb_pd_toc.jsonl & usb_pd_sections.jsonl (output/)"]
    end

    %% Validation
    subgraph Validation
        Validator["✅ ExcelValidator\n(uses Apache POI)"]
        ReportGen["📊 Report Generator\n-> validation_report.xlsx (output/)"]
    end

    %% Observability (logs NOT in output/)
    subgraph Observability
        Logger["📝 SLF4J + Logback\nwrites performance messages"]
        Perf["📈 PerfProbe / PerfLogger\n-> logs/performance.log"]
    end

    %% Outputs
    subgraph Outputs
        TOC["🗂 usb_pd_toc.jsonl (output/)"]
        SECS["🗂 usb_pd_sections.jsonl (output/)"]
        VALID["🗂 validation_report.xlsx (output/)"]
        LOG["📄 performance.log (logs/performance.log)"]
    end

    %% Flows
    Dev --> App
    App --> UploadAPI
    UploadAPI --> PDF
    PDF --> PdfBox
    PdfBox --> ToCExtractor
    PdfBox --> SectionExtractor

    ToCExtractor --> JsonWriter
    SectionExtractor --> PostProc
    PostProc --> Dedup
    Dedup --> JsonWriter

    JsonWriter --> SECS
    JsonWriter --> TOC

    ToCExtractor --> Validator
    JsonWriter --> Validator
    Validator --> ReportGen
    ReportGen --> VALID

    PdfBox --> Logger
    SectionExtractor --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> LOG

    Logger --> Perf

