```mermaid
sequenceDiagram
  actor Dev as Developer
  participant API as PdfParserController
  participant FS as File System (output/uploads)
  participant PDF as PDFBox
  participant TOC as PdfBoxTocExtractor
  participant SEC as PdfBoxSectionExtractor
  participant POST as SectionPostProcessor
  participant DEDUP as Deduplicator
  participant JSON as JacksonJsonlWriter
  participant VAL as ExcelValidator
  participant LOG as PerfLogger

  Dev->>API: POST /api/pdf/parse (file)
  API->>FS: save uploaded file
  API->>LOG: log "Upload saved"
  API->>PDF: request parse(pdf)
  PDF->>TOC: extract TOC pages
  TOC-->>PDF: tocSections
  TOC->>LOG: log "ToC extracted"
  PDF->>SEC: extract full sections
  SEC-->>PDF: allSections (Section POJOs)
  SEC->>LOG: log "Sections extracted"
  SEC->>POST: send allSections
  POST-->>SEC: cleanedSections
  POST->>DEDUP: send cleanedSections
  DEDUP-->>POST: dedupedSections
  TOC->>JSON: provide tocSections
  DEDUP->>JSON: provide dedupedSections
  JSON->>FS: write usb_pd_toc.jsonl & usb_pd_sections.jsonl
  JSON->>LOG: log "JSONL written"
  TOC->>VAL: provide usb_pd_toc.jsonl
  JSON->>VAL: provide usb_pd_sections.jsonl
  VAL->>VAL: compare and build report
  VAL->>FS: write validation_report.xlsx
  VAL->>LOG: log "Validation report written"
  LOG->>LOG: write final "Job complete" entry
  Dev->>FS: download outputs (toc, sections, report)
