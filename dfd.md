```mermaid
flowchart TD

  %% External Entity
  User["👤 Developer / Client"]

  %% Processes
  P1["P1: Receive & Save PDF\n(PdfParserController)"]
  P2["P2: Parse TOC\n(PdfBoxTocExtractor)"]
  P3["P3: Parse Sections\n(PdfBoxSectionExtractor)"]
  P4["P4: Write JSONL\n(JsonlWriter - Jackson)"]
  P5["P5: Validate TOC vs Sections\n(ExcelValidator - Apache POI)"]
  P6["P6: Logging & Performance\n(SLF4J + Logback + PerfLogger)"]

  %% Data Stores
  D_PDF["D1: Uploaded PDFs\n(output/uploads/)"]
  D_TOC["D2: TOC JSONL\n(usb_pd_toc.jsonl)"]
  D_SECS["D3: Sections JSONL\n(usb_pd_sections.jsonl)"]
  D_REPORT["D4: Validation Report\n(validation_report.xlsx)"]
  D_LOGS["D5: Logs\n(logs/performance.log)"]

  %% Flows: Input
  User -->|Upload PDF (HTTP POST)| P1
  P1 -->|validated PDF bytes| D_PDF
  P1 -->|Upload saved metrics| P6

  %% Flows: Parsing
  D_PDF -->|PDF path| P2
  D_PDF -->|PDF path| P3

  P2 -->|List<Section> tocSections| P4
  P3 -->|List<Section> allSections| P4
  P2 -->|ToC extraction metrics| P6
  P3 -->|Section extraction metrics| P6

  %% Flows: JSONL Writing
  P4 -->|write TOC JSONL| D_TOC
  P4 -->|write Sections JSONL| D_SECS
  P4 -->|JSONL write metrics| P6

  %% Flows: Validation
  D_TOC -->|TOC records| P5
  D_SECS -->|Section records| P5
  P5 -->|Generate Excel report| D_REPORT
  P5 -->|Validation metrics| P6

  %% Flows: Logging
  P6 -->|append log lines| D_LOGS

  %% Flows: Outputs to User
  D_TOC -->|download /api/pdf/results/toc| User
  D_SECS -->|download /api/pdf/results/sections| User
  D_REPORT -->|download /api/pdf/results/validation| User
  D_LOGS -->|view performance.log| User
