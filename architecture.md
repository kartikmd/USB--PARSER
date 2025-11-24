```mermaid

graph TD

  %% Entry
  subgraph Entry
    Dev["👤 Developer / Client"]
    App["🌐 Spring Boot App\nUsbPdParserApplication"]
  end

  %% Ingestion
  subgraph Ingestion
    UploadAPI["📤 PdfParserController (REST API)\nPOST /api/pdf/parse"]
    PDF["📄 Input PDF\nUSB_PD_R3_2.pdf (saved in output/)"]
  end

  %% Parsing
  subgraph Parsing
    PdfBox["📦 PDFBox Layer\n(PdfBoxTocExtractor, PdfBoxSectionExtractor)"]
    ToCExtractor["🧭 TOC Extractor\nnumbered + front-matter + annex (A,B,C…)"]
    SectionExtractor["📑 Section Extractor\nbuilds Section objects\n(content + page)"]
  end

  %% Processing
  subgraph Processing
    PostProc["🧼 SectionPostProcessor\n(clean / normalize sections)"]
    Dedup["🔁 Deduplicator\nchooseBetterSection()"]
    JsonWriter["💾 JsonlWriter (Jackson)\nwrites TOC & Sections JSONL"]
  end

  %% Validation
  subgraph Validation
    Validator["✅ ExcelValidator (Apache POI)\ncompares TOC vs Sections"]
    ReportGen["📊 Report Generator\ncreates validation_report.xlsx"]
  end

  %% Observability
  subgraph Observability
    Logger["📝 SLF4J + Logback\nperformance + error logs"]
    Perf["📈 PerfLogger\nwrites logs/performance.log"]
  end

  %% Outputs
  subgraph Outputs
    TOC["🗂 usb_pd_toc.jsonl (output/)"]
    SECS["🗂 usb_pd_sections.jsonl (output/)"]
    VALID["🗂 validation_report.xlsx (output/)"]
    LOG["📄 performance.log (logs/)"]
  end

  %% Flows
  Dev --> App
  App --> UploadAPI

  UploadAPI --> PDF
  PDF --> PdfBox

  PdfBox --> ToCExtractor
  PdfBox --> SectionExtractor

  %% TOC flow
  ToCExtractor --> JsonWriter
  JsonWriter --> TOC

  %% Sections flow
  SectionExtractor --> PostProc
  PostProc --> Dedup
  Dedup --> JsonWriter
  JsonWriter --> SECS

  %% Validation flow
  TOC --> Validator
  SECS --> Validator
  Validator --> ReportGen
  ReportGen --> VALID

  %% Logging
  UploadAPI --> Logger
  ToCExtractor --> Logger
  SectionExtractor --> Logger
  JsonWriter --> Logger
  Validator --> Logger
  Logger --> Perf
  Perf --> LOG
