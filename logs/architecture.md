```mermaid

graph TD

    %% Entry Points
    subgraph Entry
        Dev["👤 Developer / CLI"]
        App["🌐 Spring Boot Application\nUsbPdParserApplication"]
        Runner["🖥 UsbParserRunner (CLI Runner)"]
    end

    %% Ingestion
    subgraph Ingestion
        PDF["📄 Input PDF\nUSB_PD_R3_2.pdf"]
        UploadAPI["📤 PdfParserController (REST API)"]
    end

    %% Parsing Layer
    subgraph Parsing
        PdfBox["📦 PDFBox Layer\nPdfBoxSectionExtractor\nPdfBoxTocExtractor"]
        ToCExtractor["🧭 ToC Extractor\nGenerates usb_pd_toc.jsonl"]
        SectionExtractor["📑 Section Extractor\nCreates Section POJOs"]
    end

    %% Processing Layer
    subgraph Processing
        PostProc["🧼 SectionPostProcessor\nClean & normalize sections"]
        Dedup["🔁 Deduplicator\nchooseBetterSection()"]
        JsonWriter["💾 JsonlWriter (Jackson)\nWrites usb_pd_sections.jsonl"]
    end

    %% Validation
    subgraph Validation
        Validator["✅ ExcelValidator (Apache POI)\nToC ↔ Sections validation"]
        ReportGen["📊 Report Generator\nCreates validation_report.xlsx"]
    end

    %% Observability
    subgraph Observability
        Logger["📝 SLF4J + Logback"]
        Perf["📈 PerfProbe / PerfLogger"]
    end

    %% Outputs
    subgraph Artifacts
        TOC["🗂 usb_pd_toc.jsonl"]
        SECS["🗂 usb_pd_sections.jsonl"]
        VALID["🗂 validation_report.xlsx"]
        LOG["📄 performance.log"]
    end

    %% Flows
    Dev --> Runner
    App --> UploadAPI
    Runner --> PDF
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

    PdfBox --> Logger
    SectionExtractor --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> LOG
    Logger --> Perf
