```mermaid
flowchart TD
  A[PDF file(s)\nUSB_PD_R3_2...pdf] -->|PDDocument.load & PDFTextStripper| B[Text Extraction]
  B --> C[TocExtractor (PdfBoxTocExtractor)]
  B --> D[SectionExtractor (PdfBoxSectionExtractor)]

  subgraph TocExtractor
    C1[Pre-join / clean lines]
    C2[Regex parse: DOTS_PAGE / TITLE_PAGE / TABLE_VARIANTS / LETTER_SECTION]
    C3[repairNumericKeysThatHoldTables]
    C4[build Section objects]
    C1 --> C2 --> C3 --> C4
  end
  C --> E[TOC JSONL (usb_pd_toc.jsonl)]

  subgraph SectionExtractor
    D1[Detect printed page numbers]
    D2[Normalize & merge broken lines]
    D3[Heading detection: HEADING_PATTERN / LETTER_HEADING / TABLE_HEADING]
    D4[Accumulate section content]
    D5[finalizeSection → Section objects]
    D1 --> D2 --> D3 --> D4 --> D5
  end
  D --> F[Parsed Sections JSONL (usb_pd_sections.jsonl)]

  E --> G[Validator (ExcelValidator)]
  F --> G
  G --> H[ValidationResult]
  H --> I[Writes validation_report.xlsx]
  E --> J[Downstream consumers / human review]
  F --> J

  style C fill:#f9f,stroke:#333
  style D fill:#ff9,stroke:#333
  style G fill:#9ff,stroke:#333


