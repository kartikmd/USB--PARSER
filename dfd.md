```mermaid
flowchart TD
  %% External actors
  User["👤 Developer / API Client"]

  %% Ingest
  User -->|upload PDF| Api["📤 PdfParserController (REST API)"]

  %% Save & Storage
  Api -->|save file| UploadFS["📁 output/uploads/<file>.pdf"]

  %% Parsing (PDFBox)
  UploadFS -->|read bytes| PdfBox["📦 PDFBox Layer"]
  PdfBox --> ToCExtract["🧭 PdfBoxTocExtractor"]
  PdfBox --> SecExtract["📑 PdfBoxSectionExtractor"]

  %% Outputs from parsing
  ToCExtract -->|toc list| TocJson["🗂 usb_pd_toc.jsonl (output/)"]
  SecExtract -->|sections list| RawSections["📄 in-memory Section POJOs"]

  %% Processing
  RawSections --> PostProc["🧼 SectionPostProcessor"]
  PostProc --> Dedup["🔁 Deduplicator (chooseBetterSection)"]
  Dedup --> JsonWriter["💾 JsonlWriter (Jackson)"]

  %% JSONL outputs
  JsonWriter -->|write| SecJson["🗂 usb_pd_sections.jsonl (output/)"]
  ToCExtract -->|also ->| JsonWriter

  %% Validation
  TocJson --> Validator["✅ ExcelValidator (Apache POI)"]
  SecJson --> Validator
  Validator -->|write| ValidXlsx["🗂 validation_report.xlsx (output/)"]

  %% Observability / logs
  Api --> Logger["📝 SLF4J + Logback"]
  ToCExtract --> Logger
  SecExtract --> Logger
  JsonWriter --> Logger
  Validator --> Logger
  Logger -->|perf lines| PerfLog["📄 logs/performance.log"]

  %% Final downloads
  User -->|download| TocJson
  User -->|download| SecJson
  User -->|download| ValidXlsx
