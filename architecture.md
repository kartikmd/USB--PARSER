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
    PDF["📄 Uploaded PDF\n(saved in output/uploads/)"]
  end

  %% Parsing
  subgraph Parsing
    PdfBox["📦 Apache PDFBox Layer"]
    ToCExtractor["🧭 PdfBoxTocExtractor\n(Extracts TOC entries including A,B,C annexes)"]
    SectionExtractor["📑 PdfBoxSectionExtractor\n(Extracts content + page mapping)"]
  end

  %% JSONL Writing (Now merged — no processing layer)
  subgraph Serialization
    JsonWriter["💾 JsonlWriter (Jackson)\nWrites: usb_pd_toc.jsonl & usb_pd_sections.jsonl"]
  end

  %% Validation
  subgraph Validation
    Validator["✅ ExcelValidator (Apache POI)\nCompares TOC vs Extracted Sections"]
    ReportGen["📊 Report Generator\nProduces validation_report.xlsx"]
  end

  %% Observability
  subgraph Observability
    Logger["📝 SLF4J + Logback\n(app + performance logs)"]
    Perf["📈 PerfLogger\nlogs/performance.log"]
  end

  %% Outputs
  subgraph Outputs
    TOC["🗂 usb_pd_toc.jsonl (output/)"]
    SECS["🗂 usb_pd_sections.jsonl (output/)"]
    VALID["🗂 validation_report.xlsx (output/)"]
    LOG["📄 performance.log (logs/)"]
  end


  %% Flow Connections
  Dev --> App
  App --> UploadAPI

  UploadAPI --> PDF
  PDF --> PdfBox

  PdfBox --> ToCExtractor
  PdfBox --> SectionExtractor

  ToCExtractor --> JsonWriter
  SectionExtractor --> JsonWriter
  JsonWriter --> TOC
  JsonWriter --> SECS

  TOC --> Validator
  SECS --> Validator
  Validator --> ReportGen
  ReportGen --> VALID

  UploadAPI --> Logger
  ToCExtractor --> Logger
  SectionExtractor --> Logger
  JsonWriter --> Logger
  Validator --> Logger
  Logger --> Perf
  Perf --> LOG
