```mermaid
graph TD

    %% Users & Entry
    User["👤 User / Client"]
    API["🌐 Spring Boot API\n(/api/pdf/parse)"]

    %% Input
    PDF["📄 Uploaded USB-PD PDF"]

    %% Parsing Layer
    TOC_EX["🧭 PdfBoxTocExtractor"]
    SEC_EX["📑 PdfBoxSectionExtractor"]

    %% Output Serializers
    JSONW["💾 JsonlWriter\n(Jackson Streaming)"]

    %% Output Files
    TOC_JSON["🗂 usb_pd_toc.jsonl"]
    SEC_JSON["🗂 usb_pd_sections.jsonl"]

    %% Validation
    VALIDATOR["📊 ExcelValidator\n(Apache POI)"]
    REPORT["📁 validation_report.xlsx"]

    %% Logging
    LOGGING["📝 SLF4J + Logback"]
    PERFLOG["📈 performance.log"]

    %% Flow Connections
    User -->|Upload PDF (HTTP POST)| API
    API --> PDF

    PDF --> TOC_EX
    PDF --> SEC_EX

    TOC_EX --> JSONW
    SEC_EX --> JSONW

    JSONW --> TOC_JSON
    JSONW --> SEC_JSON

    TOC_JSON --> VALIDATOR
    SEC_JSON --> VALIDATOR
    VALIDATOR --> REPORT

    API --> LOGGING
    TOC_EX --> LOGGING
    SEC_EX --> LOGGING
    JSONW --> LOGGING
    VALIDATOR --> LOGGING

    LOGGING --> PERFLOG
