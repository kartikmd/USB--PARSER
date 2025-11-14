```mermaid

graph TD

    %% Environment
    subgraph Environment
        User["👤 User / Developer
        Input: USB_PD_R3_2.pdf
        Output: Runs parsing process"]

        CLI["🖥️ CLI Runner
        (UsbParserRunner / main)
        Input: PDF path
        Output: Triggers parsing & validation"]

        API["🌐 Spring Boot API
        (PdfParserController)
        Input: Upload / API request
        Output: JSON response or file output"]
    end

    %% Core Parsing Engine
    subgraph ParsingEngine
        PdfBox["📦 PDF Parser (Apache PDFBox)
        Input: PDF
        Task: Extract text blocks, layout, and headings
        Output: Raw text data"]

        ToCExtractor["🧭 ToC Extractor (PdfBoxTocExtractor)
        Input: Front-matter text
        Task: Identify numbered section titles
        Output: usb_pd_toc.jsonl"]

        SectionExtractor["📑 Section Extractor (PdfBoxSectionExtractor)
        Input: Text blocks
        Task: Split PDF pages into sections
        Output: Section objects"]

        OCR["🔎 OCR Module (Tess4J)
        Input: Image-only pages
        Task: Extract text via OCR
        Output: OCR text content"]
    end

    %% Processing Layer
    subgraph ProcessingLayer
        PostProcessor["🧼 SectionPostProcessor
        Task: Clean titles, normalize section IDs,
        remove unwanted text"]

        Deduplicator["🔁 Deduplicator
        Task: Keep best section per section_id"]

        JsonWriter["💾 JsonlWriter (Jackson)
        Task: Write usb_pd_sections.jsonl"]

        Validator["✅ ExcelValidator (Apache POI)
        Task: Compare ToC vs Sections,
        Identify missing/extra entries"]

        ReportGenerator["📊 Report Generator
        Task: Create validation_report.xlsx"]
    end

    %% Observability
    subgraph Observability
        Logger["📝 Logger (SLF4J + Logback)
        Input: Events & exceptions
        Output: performance.log"]

        Perf["📈 PerfProbe / PerfLogger
        Task: Measure execution time,
        memory, and throughput"]

        README["📚 Documentation
        Content: Architecture, Usage, Output details"]
    end

    %% Storage / Artifacts
    subgraph Storage
        TOCFile["🗂 usb_pd_toc.jsonl
        Stores: Table of Contents data"]

        SectionsFile["🗂 usb_pd_sections.jsonl
        Stores: Parsed section data"]

        ValidationFile["🗂 validation_report.xlsx
        Stores: Validation results (ToC ↔ Sections)"]

        LogFile["📄 performance.log
        Stores: Errors, warnings, and performance"]

        Archive["📦 Project Repository / Release.zip
        Includes: Code, outputs, README"]
    end

    %% Connections
    User --> CLI
    User --> API
    CLI --> PdfBox
    API --> PdfBox
    PdfBox --> ToCExtractor
    PdfBox --> SectionExtractor
    SectionExtractor --> OCR
    OCR --> SectionExtractor
    SectionExtractor --> PostProcessor
    PostProcessor --> Deduplicator
    Deduplicator --> JsonWriter
    ToCExtractor --> TOCFile
    JsonWriter --> SectionsFile
    ToCExtractor --> Validator
    JsonWriter --> Validator
    Validator --> ReportGenerator
    ReportGenerator --> ValidationFile

    %% Logging / Observability Links
    PdfBox --> Logger
    SectionExtractor --> Logger
    Validator --> Logger
    JsonWriter --> Logger
    Logger --> LogFile
    Logger --> Perf

    %% Final Artifacts
    TOCFile --> Archive
    SectionsFile --> Archive
    ValidationFile --> Archive
    LogFile --> Archive
    README --> Archive
