```mermaid
graph TD

    %% Entry Points
    subgraph Entry
        App["🌐 Spring Boot Application\nUsbPdParserApplication"]
        UploadAPI["📤 PdfParserController (REST API)"]
    end

    %% Ingestion
    subgraph Ingestion
        PDF["📄 Uploaded PDF"]
    end

    %% Parsing Layer
    subgraph Parsing
        PdfBox["📦 PDFBox Layer\n(PdfBoxTocExtractor & PdfBoxSectionExtractor)"]
        ToCExtractor["🧭 Extract TOC\n-> usb_pd_toc.jsonl"]
        SectionExtractor["📑 Extract Sections\n-> Section Objects"]
    end

    %% Processing Layer
    subgraph Processing
        PostProc["🧼 SectionPostProcessor\n(clean/normalize)"]
        Dedup["🔁 Deduplicator\nchooseBetterSection()"]
        JsonWriter["💾 JsonWriter (Jackson)\n-> usb_pd_sections.jsonl"]
    end

    %% Validation
    subgraph Validation
        Validator["✅ ExcelValidator (Apache POI)\nCompare TOC vs Sections"]
        ReportGen["📊 Generate\nvalidation_report.xlsx"]
    end

    %% Observability
    subgraph Observability
        Logger["📝 SLF4J Logger"]
        Perf["📈 PerfProbe / PerfLogger\nWrites to /logs/performance.log"]
    end

    %% Outputs
    subgraph OutputFiles
        TOC["🗂 usb_pd_toc.jsonl"]
        SECS["🗂 usb_pd_sections.jsonl"]
        VALID["🗂 validation_report.xlsx"]
        LOG["📄 logs/performance.log"]
    end


    %% Flows
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

    %% Logging flows
    UploadAPI --> Perf
    ToCExtractor --> Perf
    SectionExtractor --> Perf
    JsonWriter --> Perf
    Validator --> Perf

    Perf --> LOG
