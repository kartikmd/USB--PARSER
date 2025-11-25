```mermaid
graph TD

    %% Entry / Trigger
    User["User / Client"]
    API["Spring Boot REST API (/api/pdf/parse)"]

    %% Input
    PDF["Uploaded USB-PD PDF"]

    %% Parsing Layer
    TOC_EX["PdfBoxTocExtractor (Extracts Table of Contents)"]
    SEC_EX["PdfBoxSectionExtractor (Extracts Full Sections)"]

    %% Serialization Layer
    JSONW["JsonlWriter (Jackson Streaming)"]

    %% Output Files
    TOC_JSON["usb_pd_toc.jsonl"]
    SEC_JSON["usb_pd_sections.jsonl"]

    %% Validation
    VALIDATOR["ExcelValidator (Apache POI)"]
    REPORT["validation_report.xlsx"]

    %% Logging
    LOGGING["SLF4J + Logback"]
    PERFLOG["performance.log"]

    %% Flow Connections
    User -->|Upload PDF| API
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
