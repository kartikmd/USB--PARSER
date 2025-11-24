```mermaid
flowchart TB
  %% Top-level actors
  Dev["👤 Developer / Client"]
  App["🌐 Spring Boot App\nUsbPdParserApplication"]
  UploadAPI["📤 PdfParserController (REST API)\nPOST /api/pdf/parse"]

  %% Ingestion
  PDF["📄 Input PDF\nUSB_PD_R3_2.pdf (output/)"]
  PdfBox["📦 Apache PDFBox\n(page extraction)"]

  %% Parsing (INTERNAL processing included)
  subgraph Parsing["📑 Parsing Layer (extractors + processing)"]
    ParsingService["🔧 ParsingService\n(TextNormalizer, PreJoin/Merge, RegexEngine,\nPrintedPageDetection, TableRepair, Hyphenation, Dotted-line skip)"]
    ToCExtractor["🧾 PdfBoxTocExtractor\n(uses ParsingService)"]
    SectionExtractor["📄 PdfBoxSectionExtractor\n(uses ParsingService)"]
  end

  %% Writers, Validation, Observability
  JsonWriter["💾 JsonWriter\n(writes usb_pd_toc.jsonl & usb_pd_sections.jsonl)"]
  Validator["✅ ExcelValidator\n(Comparisons + report via Apache POI)"]
  ReportGen["📊 Report Generator\n(validation_report.xlsx)"]
  Logger["📝 SLF4J + Logback"]
  Perf["📈 PerfLogger\n(logs/performance.log)"]

  %% Outputs
  TOC["🗂 usb_pd_toc.jsonl (output/)"]
  SECS["🗂 usb_pd_sections.jsonl (output/)"]
  VALID["🗂 validation_report.xlsx (output/)"]
  LOG["📄 performance.log (logs/)"]

  %% Flows
  Dev --> App
  App --> UploadAPI
  UploadAPI --> PDF
  PDF --> PdfBox

  %% Injection / parsing wiring
  PdfBox --> ParsingService
  ParsingService --> ToCExtractor
  ParsingService --> SectionExtractor

  %% Extractors -> Writer -> Outputs
  ToCExtractor --> JsonWriter
  SectionExtractor --> JsonWriter
  JsonWriter --> TOC
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
