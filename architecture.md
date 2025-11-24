```mermaid
flowchart TB
    %% Actors
    subgraph DevEnv["Developer"]
        Dev["👨‍💻 Developer"]
    end

    subgraph AppEnv["Application"]
        App["🌐 App (Spring Boot)"]
        UploadAPI["📥 Upload API\n(/upload)"]
        PdfBox["📦 Apache PDFBox"]
        ToCExtractor["🧾 PdfBoxTocExtractor"]
        SectionExtractor["📑 PdfBoxSectionExtractor"]
        PostProc["🔧 Sections Post-Proc"]
        Dedup["🧹 Deduplicate / Repair"]
        JsonWriter["💾 JSON Writer"]
    end

    %% Validation
    subgraph Validation
        Validator["✅ ExcelValidator\n(Apache POI)"]
        ReportGen["📊 Report Generator\n(writes validation_report.xlsx)"]
    end

    %% Observability
    subgraph Observability
        Logger["📝 SLF4J + Logback"]
        Perf["📈 PerfLogger\nlogs/performance.log"]
    end

    %% Outputs
    subgraph Outputs
        TOC["🗂 usb_pd_toc.jsonl\n(output/)"]
        SECS["🗂 usb_pd_sections.jsonl\n(output/)"]
        VALID["🗂 validation_report.xlsx\n(output/)"]
        LOG["📄 performance.log\n(logs/)"]
    end

    %% File reference (project zip)
    NoteFile["📦 Project ZIP:\n/mnt/data/testing.zip"]:::noteStyle

    %% Flows (as requested)
    Dev --> App
    App --> UploadAPI

    UploadAPI --> NoteFile
    UploadAPI --> PDF[".pdf payload"]
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

    classDef noteStyle fill:#f8f0c8,stroke:#b08b00;
