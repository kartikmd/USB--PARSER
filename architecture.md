```mermaid
graph TD
  %% Entry
  subgraph ENTRY
    Dev["👤 Developer / CLI"]
    App["🌐 Spring Boot App\nUsbPdParserApplication"]
    Runner["🖥 UsbParserRunner (CLI)"]
  end

  %% Ingestion
  subgraph INGESTION
    PDF["📄 Input PDF\nUSB_PD_R3_2.pdf"]
    UploadAPI["📤 PdfParserController (REST API)"]
  end

  %% Parsing
  subgraph PARSING
    PdfBox["📦 PDFBox Layer\n(PdfBoxTocExtractor, PdfBoxSectionExtractor)"]
    ToCExtractor["🧭 ToC Extractor\n-> usb_pd_toc.jsonl"]
    SectionExtractor["📑 Section Extractor\n-> Section POJOs"]
  end

  %% Processing
  subgraph PROCESSING
    PostProc["🧼 SectionPostProcessor\n(normalize/merge)"]
    Dedup["🔁 Deduplicator\n(chooseBetterSection)"]
    JsonWriter["💾 JsonlWriter (Jackson)\n-> usb_pd_sections.jsonl"]
  end

  %% Validation
  subgraph VALIDATION
    Validator["✅ ExcelValidator\n(uses Apache POI)"]
    ReportGen["📊 Report Generator\n-> validation_report.xlsx"]
  end

  %% Observability
  subgraph OBSERVABILITY
    Logger["📝 SLF4J + Logback"]
    Perf["📈 PerfProbe / PerfLogger\n-> performance.log"]
  end

  %% Outputs
  subgraph OUTPUTS
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
