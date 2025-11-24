```mermaid
flowchart TB

  %% Top-level actors
  Dev["👤 Developer / Client"]
  App["🌐 Spring Boot App\nUsbPdParserApplication"]
  
  %% Ingestion Layer inside Controller
  subgraph Ingestion["📥 Ingestion Layer (Inside Controller)"]
    UploadAPI["📤 PdfParserController (REST API)\nPOST /api/pdf/parse"]
    SavedPDF["💾 Saved File\n(output/uploaded.pdf)"]
  end

  %% PDFBox + Parsing Layer
  subgraph PDFProcessing["📦 PDF Processing"]
    PdfBox["📦 Apache PDFBox\n(load + extract pages)"]

    subgraph Parsing["📑 Internal Parsing Layer (inside PDFBox)"]
      ParsingService["🔧 ParsingService\n(TextNormalizer, PreJoin/Merge,\nRegexEngine, PrintedPageDetection,\nTableRepair, Hyphenation, Dotted-line skip)"]
      ToCExtractor["🧾 PdfBoxTocExtractor\n(uses ParsingService)"]
      SectionExtractor["📄 PdfBoxSectionExtractor\n(uses ParsingService)"]
    end
  end

  %% Outputs & Validation
  JsonWriter["💾 JsonWriter\n(writes jsonl outputs)"]
  Validator["✅ ExcelValidator\n(compare TOC vs Sections)"]
  ReportGen["📊 Report Generator\n(validation_report.xlsx)"]

  %% Observability
  Logger["📝 SLF4J + Logback"]
  Perf["📈 PerfLogger\n(logs/performance.log)"]

  %% Output files
  TOC["🗂 usb_pd_toc.jsonl (output/)"]
  SECS["🗂 usb_pd_sections.jsonl (output/)"]
  VALID["🗂 validation_report.xlsx (output/)"]
  LOG["📄 performance.log (logs/)"]

  %% Flow Connections
  Dev --> App
  App --> UploadAPI

  UploadAPI --> SavedPDF
  SavedPDF --> PdfBox

  %% PDFBox → Internal Parsing Layer
  PdfBox --> ParsingService
  ParsingService --> ToCExtractor
  ParsingService --> SectionExtractor

  %% Extractors → Writer → Output
  ToCExtractor --> JsonWriter
  SectionExtractor --> JsonWriter

  JsonWriter --> TOC
  JsonWriter --> SECS

  %% Validation
  TOC --> Validator
  SECS --> Validator
  Validator --> ReportGen
  ReportGen --> VALID

  %% Logging
  UploadAPI --> Logger
  PdfBox --> Logger
  ParsingService --> Logger
  ToCExtractor --> Logger
  SectionExtractor --> Logger
  JsonWriter --> Logger
  Validator --> Logger
  Logger --> Perf
  Perf --> LOG

  Perf --> LOG
