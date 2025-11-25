USB Power Delivery (USB-PD) Parsing System — Task Documentation
📌 Project Overview
This system extracts structured and machine-readable information from the official USB Power Delivery Specification PDF, and generates the following outputs:

usb_pd_toc.jsonl → Table of Contents extracted from PDF

usb_pd_sections.jsonl → All parsed sections with content

validation_report.xlsx → Comparison between ToC and extracted sections

performance.log → Execution timing and performance metrics

The application is implemented using:

Java 17 · Spring Boot · Apache PDFBox · Apache POI · Jackson JSONL · SLF4J + Logback

🎯 Objectives
The system performs the following processing steps end-to-end:

1️⃣ PDF Input & Validation
Accepts PDF file via REST API (Multipart Upload).

Validates:
✔ File exists
✔ File format is .pdf

Stores a copy under:

output/uploads/
2️⃣ Extract Table of Contents (TOC)
Using PdfBoxTocExtractor, the system identifies:

Numbered sections (e.g., 1 , 1.2, 3.4.5)

Annex headings (A, B, C…)

Titles and page numbers

Section hierarchy levels

Each entry contains:

Field	Description
section_id	Unique ID like 1.2.3 or A
title	Human-readable heading
page	Printed page number
level	Depth of hierarchy
parent_id	Parent section
full_path	Combination of id + title
📄 Output → usb_pd_toc.jsonl

3️⃣ Extract Full Sections
Using PdfBoxSectionExtractor, the system extracts:

Heading text

Printed or fallback PDF page number

Section content (paragraphs, text following the heading)

📄 Output → usb_pd_sections.jsonl

4️⃣ Write Outputs in JSONL Format
A single object per line is written using streaming JSON writing (efficient for large files).

Files generated:

output/
 ├─ usb_pd_toc.jsonl
 └─ usb_pd_sections.jsonl
5️⃣ Validate TOC vs Extracted Sections
Using ExcelValidator (Apache POI), the tool checks:

Validation Check	Purpose
Missing sections	Exists in ToC but not extracted
Extra sections	Extracted but not in ToC
Page mismatch	Page number variation
Hierarchy consistency	Section structure validation
📄 Output → validation_report.xlsx

6️⃣ Logging & Performance Monitoring
Used technologies:

SLF4J + Logback → Info and error logs

PerfLogger → Measures execution time, memory usage & stage timings

📄 Final log stored in:

logs/performance.log
