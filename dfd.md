```mermaid
flowchart TD

  %% External Entity
  User["👤 Developer / Client"]

  %% Processes
  P1["P1: Upload & Validate PDF\n(PdfParserController)"]
  P2["P2: Extract TOC\n(PdfBoxTocExtractor)"]
  P3["P3: Extract Sections\n(PdfBoxSectionExtractor)"]
  P4["P4: Write JSONL\n(JsonlWriter - Jackson)"]
  P5["P5: Validate TOC vs Sections\n(ExcelValidator - Apache POI)"]
  P6["P6: Logging & Performance\n(SLF4J + Logback + PerfLogger)"]

  %% Data Stores
  D_PDF["D1: Uploaded PDF\n(output/uploads/)"]
  D_TOC["D2: TOC JSONL\nusb_pd_toc.jsonl (output/)"]
  D_SECS["D3: Sections JSONL\nusb_pd_sections.jsonl (output/)"]
  D_REPORT["D4: Validation Report\nvalidation_report.xlsx (output/)"]
  D_LOGS["D5: Logs\nlogs/performance.log"]

  %% Flows: User ↔ System
  User -->|Upload PDF| P1
  P1 -->|Success / Status| User

  %% P1: Upload & store PDF
  P1 -->|valid PDF bytes| D_PDF
  P1 -->|\"Upload saved...\" metrics| P6

  %% P2: TOC extraction
  D_PDF -->|PDF file| P2
  P2 -->|TOC entries (List<Section>)| P4
  P2 -->|\"ToC extracted...\" metrics| P6

  %% P3: Sections extraction
  D_PDF -->|PDF file| P3
  P3 -->|Section entries (List<Section>)| P4
  P3 -->|\"Sections extracted...\" metrics| P6

  %% P4: JSONL writing
  P4 -->|write TOC JSONL| D_TOC
  P4 -->|write Sections JSONL| D_SECS
  P4 -->|\"JSONL written...\" metrics| P6

  %% P5: Excel validation
  D_TOC -->|TOC records| P5
  D_SECS -->|Section records| P5
  P5 -->|validation_report.xlsx| D_REPORT
  P5 -->|\"Validation report written...\" metrics| P6

  %% P6: Logging
  P6 -->|append logs| D_LOGS

  %% Outputs back to user
  D_TOC -->|download TOC JSONL| User
  D_SECS -->|download Sections JSONL| User
  D_REPORT -->|download Excel report| User
  D_LOGS -->|view performance.log| User

