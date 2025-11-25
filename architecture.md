```mermaid
graph TD

  %% Entry Layer
  subgraph Entry
    Dev["👤 Developer / Client"]
    App["🌐 Spring Boot App\nUsbPdParserApplication"]
  end

  %% Ingestion Layer
  subgraph Ingestion
    UploadAPI["📤 PdfParserController (REST API)\nPOST /api/pdf/parse"]
    PDF["📄 Input PDF\n(saved in output/)"]
  end

  %% Parsing Layer (includes all internal processing)
  subgraph Parsing
    PdfBox["📦 Apache PDFBox\n(load pages + extract text)"]

    ToCExtractor["🧾 PdfBoxTocExtractor\n(parse TOC → section_id, title, page)"]
    SectionExtractor["📄 PdfBoxSectionExtractor\n(parse headings → content + page)"]

    ParsingService["🔧 Internal Parsing Helpers\n• normalize text\n• pre-join broken lines\n• printed page detection\n• skip headers/footers\n• hyphenation handling\n• dotted leader removal\n• table-id repair"]
  end

  %% Writer Layer
  subgraph Writer
    JsonWriter["💾 JsonlWriter\nwrites TOC & Sections JSONL"]
  end

  %% Validation Layer
  subgraph Validation
    Validator["✅ ExcelValidator (Apache POI)\ncompares TOC vs Sections"]
    ReportGen["📊 Report Generator\ncreates validation_report.xlsx"]
  end

  %% Observability Layer
  subgraph Observability
    Logger["📝 SLF4J + Logback\nerror/info/debug logs"]
    Perf["📈 PerfLogger\nwrites logs/performance.log"]
  end

  %% Outputs Layer
  subgraph Outputs
    TOC["🗂 usb_pd_toc.jsonl"]
    SECS["🗂 usb_pd_sections.jsonl"]
    VALID["🗂 validation_report.xlsx"]
    LOG["📄 performance.log"]
  end

  %% Flows
  Dev --> App
  App --> UploadAPI

  UploadAPI --> PDF
  PDF --> PdfBox

  %% Parsing connections
  PdfBox --> ParsingService
  ParsingService --> ToCExtractor
  ParsingService --> SectionExtractor

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
