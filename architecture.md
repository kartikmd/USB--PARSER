```mermaid
flowchart TB
  %% Top-level actors
  Dev["👨‍💻 Developer"]
  App["🌐 Spring Boot App"]
  UploadAPI["📥 Upload API (/upload)"]

  %% PDF ingestion
  PDF["📄 PDF File"]
  PdfBox["📦 Apache PDFBox\n(page extraction)"]

  %% Injection / Parsing Layer (single injectable service)
  subgraph InjectionLayer["🧩 Injection / Parsing Layer"]
    DI["⚙️ DI Container\n(creates ParsingService)"]
    ParsingService["🔧 ParsingService\n(TextNormalizer, PreJoin, RegexEngine, TableRepair)"]
  end

  %% Extractors & Writers
  ToCExtractor["🧾 PdfBoxTocExtractor\n(uses ParsingService)"]
  SectionExtractor["📑 PdfBoxSectionExtractor\n(uses ParsingService)"]
  JsonWriter["💾 JsonWriter\n(writes jsonl outputs)"]

  %% Validation & Reporting
  Validator["✅ ExcelValidator\n(Apache POI)"]
  ReportGen["📊 Report Generator\n(validation_report.xlsx)"]

  %% Observability
  Logger["📝 SLF4J + Logback"]
  Perf["📈 PerfLogger\n(logs/performance.log)"]

  %% Outputs
  TOC["🗂 usb_pd_toc.jsonl (output/)"]
  SECS["🗂 usb_pd_sections.jsonl (output/)"]
  VALID["🗂 validation_report.xlsx (output/)"]
  LOG["📄 performance.log (logs/)"]

  %% Flows / Connections
  Dev --> App
  App --> UploadAPI
  UploadAPI --> PDF
  PDF --> PdfBox

  %% Injection wiring
  PdfBox --> DI
  DI --> ParsingService
  ParsingService --> ToCExtractor
  ParsingService --> SectionExtractor

  %% Extraction flow
  ToCExtractor --> JsonWriter
  JsonWriter --> TOC

  SectionExtractor --> JsonWriter
  JsonWriter --> SECS

  %% Validation
  TOC --> Validator
  SECS --> Validator
  Validator --> ReportGen
  ReportGen --> VALID

  %% Logging / Observability
  UploadAPI --> Logger
  PdfBox --> Logger
  ParsingService --> Logger
  ToCExtractor --> Logger
  SectionExtractor --> Logger
  JsonWriter --> Logger
  Validator --> Logger
  Logger --> Perf
  Perf --> LOG
