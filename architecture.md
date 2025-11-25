```mermaid
flowchart TB
  %% Entry
  subgraph Entry
    Dev["👤 Developer / Client"]
    App["🌐 Spring Boot App\nUsbPdParserApplication"]
  end

  %% Ingestion
  subgraph Ingestion
    UploadAPI["📤 PdfParserController (REST API)\nPOST /api/pdf/parse"]
    SavedPDF["💾 Saved PDF\n(output/uploaded.pdf)"]
  end

  %% PDF + Parsing (all processing inside parsing layer)
  subgraph Parsing["📑 Parsing Layer (PDFBox + internal processing)"]
    PdfBox["📦 Apache PDFBox\n(load pages & extract text)"]
    Helpers["🔧 Internal Helpers\n(normalize / pre-join / printed-page detection\nhyphenation join / dotted-leader removal\ntable-id repair)"]
    ToC["🧾 PdfBoxTocExtractor\n(extract sectionId,title,page)"]
    Section["📄 PdfBoxSectionExtractor\n(find headings and collect content)"]
  end

  %% Writers & Validation
  JsonWriter["💾 JsonWriter\nwrites usb_pd_toc.jsonl & usb_pd_sections.jsonl"]
  Validator["✅ ExcelValidator\n(compares TOC ↔ Sections & produces XLSX)"]
  ReportGen["📊 Report Generator\n(validation_report.xlsx)"]

  %% Observability
  Logger["📝 SLF4J + Logback\n(app logs & debug)"]
  Perf["📈 PerfLogger\n(writes logs/performance.log)"]

  %% Outputs
  TOC["🗂 usb_pd_toc.jsonl (output/)"]
  SECS["🗂 usb_pd_sections.jsonl (output/)"]
  VALID["🗂 validation_report.xlsx (output/)"]
  LOG["📄 performance.log (logs/)"]

  %% Flows
  Dev --> App
  App --> UploadAPI
  UploadAPI --> SavedPDF
  SavedPDF --> PdfBox

  %% Inside parsing layer wiring (helpers are used by extractors)
  PdfBox --> Helpers
  Helpers --> ToC
  Helpers --> Section
  PdfBox --> ToC
  PdfBox --> Section

  %% Extractors -> Writer -> Outputs
  ToC --> JsonWriter
  Section --> JsonWriter
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
  Helpers --> Logger
  ToC --> Logger
  Section --> Logger
  JsonWriter --> Logger
  Validator --> Logger
  Logger --> Perf
  Perf --> LOG

  Perf --> LOG
