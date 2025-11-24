```mermaid

flowchart TB

    %% User & Application
    User["👤 User / Developer"]
    App["🌐 Spring Boot Application"]
    UploadAPI["📥 /upload API"]

    %% PDF Processing
    PDF["📄 Input PDF"]
    PdfBox["📦 PDFBox\n(read pages & extract text)"]

    %% Extractors
    TocExt["🧾 TOC Extractor"]
    SecExt["📑 Section Extractor"]

    %% Outputs
    TocJSON["🗂 usb_pd_toc.jsonl"]
    SecJSON["🗂 usb_pd_sections.jsonl"]

    %% Validation
    Validator["✅ ExcelValidator"]
    Report["📊 validation_report.xlsx"]

    %% Logging
    Logger["📝 Logback Logging"]
    PerfLog["📈 performance.log"]

    %% Flows
    User --> App
    App --> UploadAPI
    UploadAPI --> PDF
    PDF --> PdfBox

    PdfBox --> TocExt
    PdfBox --> SecExt

    TocExt --> TocJSON
    SecExt --> SecJSON

    TocJSON --> Validator
    SecJSON --> Validator

    Validator --> Report

    %% Logging flow
    App --> Logger
    TocExt --> Logger
    SecExt --> Logger
    Validator --> Logger
    Logger --> PerfLog
