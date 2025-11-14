USB Power Delivery (USB-PD) Parsing Assignment – Task Documentation
📌 Project Overview
This project extracts structured information from the official USB Power Delivery Specification PDF, generating:

Table of Contents JSONL

Parsed Sections JSONL

Validation Report (Excel) comparing ToC vs sections

Performance logs and architecture documentation

The system is implemented using Java 17, Spring Boot, Apache PDFBox, Apache POI, and SLF4J/Logback.

🎯 Objectives
Your task was to build an end-to-end system that:

1. Reads the USB-PD Specification PDF
   Accepts input through CLI or REST API.

Validates file format and path.

2. Extracts ToC (Table of Contents)
   Parse numbered section titles (e.g., 1., 1.2, 3.4.5, etc.)

Generate structured TOC data:

section_id

title

page

level

parent_id

full_path

→ Output stored as usb_pd_toc.jsonl

3. Extracts Sections from PDF
   Split PDF pages into meaningful section-level content.

Combine:

heading detection,

page number tracking,

hierarchical mapping.

→ Output stored as usb_pd_sections.jsonl

4. Clean & Post-Process Sections
   Implemented optional SectionPostProcessor to:

normalize headings,

remove trash content,

unify formatting.

5. Deduplicate Sections
   Use heuristics to pick best content per section_id:

Prefer non-placeholder content.

Prefer longer text.

Prefer lower page number.

6. Write JSONL Outputs
   Use Jackson streaming to write:

One JSON object per line

Proper UTF-8 encoding

Minimal memory footprint

7. Validate ToC vs Sections
   Validation included:

Missing sections

Extra sections

Page mismatch detection

Orphaned sections

Structure consistency checks

→ Results written to validation_report.xlsx using Apache POI.

8. Logging & Performance
   SLF4J + Logback logging.

PerfProbe recorded processing times, section counts, and performance metrics.

→ Output stored in:

performance.log

