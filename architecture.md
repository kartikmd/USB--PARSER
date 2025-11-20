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
        PDF["📄 Input PDF\nUSB_PD_R3_2.pdf (saved inside output/)"]
    end

    %% Parsing
    subgraph Parsing
        PdfBox["📦 PDFBox Layer\n(PdfBoxTocExtractor, PdfBoxSectionExtractor)"]
        ToCExtractor["🧭 TOC Extractor\nProduces TOC data"]
        SectionExtractor["📑 Section Extractor\nProduces Section POJOs"]
    end

    %% Processing
    subgraph Processing
        PostProc["🧼 SectionPostProcessor\n(normalize / merge)"]
        Dedup["🔁 Deduplicator\nchooseBetterSection()"]
        JsonWriter["💾 JsonlWriter (Jackson)\nWrites TOC + Sections as JSONL"]
    end

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
    Dev --> App
    App --> UploadAPI

    UploadAPI --> PDF
    PDF --> PdfBox

    PdfBox --> ToCExtractor
    PdfBox --> SectionExtractor

    %% TOC flow
    ToCExtractor --> JsonWriter
    JsonWriter --> TOC

    %% Sections flow
    SectionExtractor --> PostProc
    PostProc --> Dedup
    Dedup --> JsonWriter
    JsonWriter --> SECS

    %% Validation flow (IMPORTANT FIX)
    TOC --> Validator
    SECS --> Validator
    Validator --> ReportGen
    ReportGen --> VALID

    %% Logging (CORRECTED)
    UploadAPI --> Logger
    ToCExtractor --> Logger
    SectionExtractor --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> Perf
    Perf --> LOG


