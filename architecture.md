```mermaid
graph TD
  %% Entry
  subgraph ENTRY
    Dev["👤 Developer / CLI / API Client"]
    App["🌐 Spring Boot App\nUsbPdParserApplication"]
  end

  %% Ingestion
  subgraph INGESTION
    UploadAPI["📤 PdfParserController (REST API)"]
    Save["💾 Save uploaded PDF\n(outDir = storage.base-path)"]
    LogUpload["📝 PerfLogger: 'Upload saved'"]
  end

  %% Parsing
  subgraph PARSING
    PdfBox["📦 PDFBox Layer"]
    ToCExt["🧭 PdfBoxTocExtractor\n-> usb_pd_toc.jsonl"]
    SecExt["📑 PdfBoxSectionExtractor\n-> Section POJOs"]
    LogToC["📝 PerfLogger: 'ToC extracted'"]
    LogSections["📝 PerfLogger: 'Sections extracted'"]
  end

  %% Processing & Output
  subgraph PROCESSING
    PostProc["🧼 SectionPostProcessor (clean/normalize)"]
    Dedup["🔁 Deduplicator (chooseBetterSection)"]
    JsonWriter["💾 JacksonJsonlWriter\n-> usb_pd_sections.jsonl"]
    LogJson["📝 PerfLogger: 'JSONL written'"]
  end

  %% Validation
  subgraph VALIDATION
    Validator["✅ ExcelValidator (Apache POI)"]
    Report["📊 validation_report.xlsx"]
    LogValid["📝 PerfLogger: 'Validation report written'"]
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

  %% Flows (ordered, matching logs)
  Dev --> App
  App --> UploadAPI
  UploadAPI --> Save
  Save --> LogUpload
  LogUpload --> PdfBox
  PdfBox --> ToCExt
  ToCExt --> LogToC
  PdfBox --> SecExt
  SecExt --> LogSections
  SecExt --> PostProc
  PostProc --> Dedup
  Dedup --> JsonWriter
  JsonWriter --> LogJson
  ToCExt --> Validator
  JsonWriter --> Validator
  Validator --> Report
  Report --> LogValid
  LogUpload --> Perf
  LogToC --> Perf
  LogSections --> Perf
  LogJson --> Perf
  LogValid --> Perf

  %% Artifacts
  ToCExt --> TOC
  JsonWriter --> SECS
  Report --> VALID
  Perf --> LOG

