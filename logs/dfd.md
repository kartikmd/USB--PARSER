```mermaid
flowchart TD
  %% External entities
  User["👤 User / Developer"]
  APIClient["🌐 API Client (optional)"]

  %% Processes
  P1["P1: Receive & Ingest PDF\n(CLI / API)"]
  P2["P2: Extract ToC\n(PdfBoxTocExtractor)"]
  P3["P3: Extract Sections\n(PdfBoxSectionExtractor)"]
  P4["P4: Post-process Sections\n(SectionPostProcessor)"]
  P5["P5: Deduplicate & Choose Best\n(chooseBetterSection)"]
  P6["P6: Write JSONL\n(JsonlWriter)"]
  P7["P7: Validate ToC vs Sections\n(ExcelValidator)"]
  P8["P8: Generate Report\n(Report Generator)"]
  P9["P9: Logging & Metrics\n(PerfProbe / Logback)"]

  %% Data stores
  D_TOC["D1: TOC Store\n(usb_pd_toc.jsonl)"]
  D_SECS["D2: Sections Store\n(usb_pd_sections.jsonl)"]
  D_REPORT["D3: Validation Reports\n(validation_report.xlsx)"]
  D_LOGS["D4: Logs & Perf\n(performance.log)"]

  %% Flows
  User -->|PDF file| P1
  APIClient -->|Upload| P1

  P1 -->|raw PDF bytes| P2
  P1 -->|raw PDF bytes| P3

  P2 -->|parsed ToC entries| D_TOC
  P2 -->|parsed ToC entries| P7

  P3 -->|raw sections| P4
  P4 -->|cleaned sections| P5
  P5 -->|deduplicated sections| D_SECS
  P5 -->|deduplicated sections| P6
  P6 -->|usb_pd_sections.jsonl| D_SECS

  P6 -->|sections file| P7
  P7 -->|validation results| D_REPORT
  P7 -->|validation feedback| P8
  P8 -->|validation_report.xlsx| D_REPORT

  %% Observability flows
  P1 -->|ingest metrics| P9
  P2 -->|parsing metrics| P9
  P3 -->|parsing metrics| P9
  P5 -->|dedupe metrics| P9
  P7 -->|validation metrics| P9
  P9 -->|logs & metrics| D_LOGS

  %% End-user outputs
  D_TOC -->|download| User
  D_SECS -->|download| User
  D_REPORT -->|download| User
  D_LOGS -->|view| User
