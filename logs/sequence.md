```mermaid
sequenceDiagram
    participant User
    participant Runner as UsbParserRunner
    participant PdfBox as PdfBoxLayer
    participant ToC as ToCExtractor
    participant SecExt as SectionExtractor
    participant Post as SectionPostProcessor
    participant Dedup as Deduplicator
    participant Json as JsonlWriter
    participant Validator
    participant Report as ReportGenerator

    User->>Runner: run(parse request with PDF path)
    Runner->>PdfBox: load PDF & extract pages
    PdfBox->>ToC: extract front-matter ToC lines
    ToC-->>Runner: toc entries (usb_pd_toc.jsonl)
    PdfBox->>SecExt: extract headings & page text
    SecExt->>Post: send raw sections
    Post->>Dedup: send cleaned sections
    Dedup->>Json: send deduped sections
    Json-->>Runner: write usb_pd_sections.jsonl (ack)
    Runner->>Validator: provide toc + sections
    Validator->>Report: validate & generate validation_report.xlsx
    Report-->>Runner: report produced
    Runner-->>User: finish (files: toc, sections, validation)
