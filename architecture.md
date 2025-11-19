```mermaid
graph TD

    %% Entry
    subgraph ENTRY
        App["🌐 Spring Boot Application\nUsbPdParserApplication"]
        UploadAPI["📤 PdfParserController (REST API)\nPOST /api/pdf/parse"]
    end

    %% Ingestion
    subgraph INGESTION
        PDF["📄 Uploaded PDF File"]
        Storage["📂 storage.base-path (output folder)\nStores uploaded PDF"]
    end

    %% Parsing Layer
    subgraph PARSING
        PdfBox["📦 PDFBox Engine\nReads PDF text"]
        ToCExtractor["🧭 PdfBoxTocExtractor\nExtract TOC → usb_pd_toc.jsonl"]
        SectionExtractor["📑 PdfBoxSectionExtractor\nExtract all sections"]
    end

    %% Processing Layer
    subgraph PROCESSING
        PostProc["🧼 SectionPostProcessor\nClean/normalize sections"]
        Dedup["🔁 Deduplication\nchooseBetterSection()"]
        JsonWriter["💾 JsonlWriter (Jackson)\nWrites final JSONL"]
    end

    %% Validation
    subgraph VALIDATION
        Validator["✅ ExcelValidator (Apache POI)\nCompare TOC ↔ Sections"]
        ReportGen["📊 Excel Report Generator\nvalidation_report.xlsx"]
    end

    %% Observability
    subgraph OBSERVABILITY
        Logger["📝 SLF4J + Logback"]
        Perf["📈 PerfLogger\nWrites performance.log\nstored in /logs"]
    end

    %% Outputs
    subgraph OUTPUTS
        TOC["🗂 usb_pd_toc.jsonl"]
        SECS["🗂 usb_pd_sections.jsonl"]
        VALID["🗂 validation_report.xlsx"]
        LOG["📄 performance.log (in logs folder)"]
    end

    %% Flow Connections
    App --> UploadAPI
    UploadAPI --> PDF
    PDF --> Storage

    Storage --> PdfBox
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

    PdfBox --> Logger
    SectionExtractor --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> LOG

    Logger --> Perf

