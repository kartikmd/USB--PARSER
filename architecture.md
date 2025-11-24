```mermaid
flowchart TB
  %% Top: Actor
  Dev["👤 Developer / Client"]

  %% Application
  App["🌐 Spring Boot App\nUsbPdParserApplication"]

  %% Ingestion Layer (inside Controller)
  subgraph Ingestion["📥 Ingestion Layer"]
    UploadAPI["📤 PdfParserController\nPOST /api/pdf/parse\n(save uploaded PDF)"]
    SavedPDF["💾 Saved PDF\n/output/uploaded.pdf"]
  end

  %% PDF / Parsing Layer (core parsing + internal processing)
  subgraph Parsing["📑 PDF Processing & Parsing Layer"]
    PdfBox["📦 Apache PDFBox\n(load + page text)"]
    ParsingService["🔧 ParsingService (injectable)\n• normalize(), pre-join/merge\n• printed page detection\n• hyphenation join\n• dotted-leader removal\n• table-id repair"]
    ToCExtractor["🧾 PdfBoxTocExtractor\n(extract TOC lines → section_id,title,page)"]
    SectionExtractor["📄 PdfBoxSectionExtractor\n(find headings → collect section content)"]
  end

  %% Output & Validation
  JsonWriter["💾 JsonWriter\nwrites usb_pd_toc.jsonl & usb_pd_sections.jsonl"]
  Validator["✅ ExcelValidator\n(compares TOC vs Sections)"]
  ReportGen["📊 Report Generator\n(validation_report.xlsx)"]

  %% Observability
  Logger["📝 SLF4J + Logback"]
  Perf["📈 PerfLogger\n(logs/performance.log)"]

  %% Output files
  TOC["🗂 usb_pd_toc.jsonl (output/)"]
  SECS["🗂 usb_pd_sections.jsonl (output/)"]
  VALID["🗂 validation_report.xlsx (output/)"]
  LOG["📄 performance.log (logs/)"]

  %% Flows
  Dev --> App
  App --> UploadAPI
  UploadAPI --> SavedPDF
  SavedPDF --> PdfBox

  %% Internal parsing wiring
  PdfBox --> ParsingService
  ParsingService --> ToCExtractor
  ParsingService --> SectionExtractor

  ToCExtractor --> JsonWriter
  SectionExtractor --> JsonWriter

  JsonWriter --> TOC
  JsonWriter --> SECS

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
