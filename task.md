📌 USB Power Delivery (USB-PD) Parsing System — Task Documentation

🧾 Overview
This project automates the extraction of structured data from the official USB Power Delivery Specification PDF.
It converts the document into machine-readable structured outputs and validates consistency between the Table of Contents (TOC) and the extracted Sections.

The system is implemented using:

Java 17
Spring Boot
Apache PDFBox
Apache POI
Jackson JSONL Writer
SLF4J + Logback

🎯 Project Goals
Task	Status
Accept PDF via REST API	✅ Done
Extract Table of Contents	✅ Done
Extract full document sections (text + hierarchy)	✅ Done
Serialize outputs into JSONL	✅ Done
Validate TOC vs Section extraction	✅ Done
Generate excel validation report	✅ Done
Maintain performance logs	✅ Done

🧩 Functional Breakdown
1️⃣ Input Handling

PDF uploaded via endpoint:
POST /api/pdf/parse
File validation: required, must be PDF

File stored to:
output/uploads/<filename>.pdf
Logs first performance entry:

Upload saved: <time> ms, CPU %, Memory Used
2️⃣ Parsing Layer (Apache PDFBox)
📍 TOC Extraction (PdfBoxTocExtractor)
Extracts:
Section number (1, 1.2, 3.4.5…)
Title
Page number
Nested hierarchy level
Parent section
➡ Output → usb_pd_toc.jsonl

📍 Content Extraction (PdfBoxSectionExtractor)
Detects headings
Extracts text belonging to each section
Handles page numbering from printed pages inside PDF
➡ Output → usb_pd_sections.jsonl

3️⃣ Serialization (JSONL Output)
Performed using:
JsonlWriter (Jackson)
Files generated:
File	Purpose
usb_pd_toc.jsonl	Raw structured Table of Contents
usb_pd_sections.jsonl	Parsed full document sections

4️⃣ Validation Layer (Excel Report)
ExcelValidator (Apache POI) compares:
Missing entries
Extra entries
Page alignment mismatch
Orphaned or unreferenced sections

➡ Produces:
validation_report.xlsx
5️⃣ Observability
Logging handled by:

SLF4J + Logback
PerfLogger
Logs include:
Upload processing time
Parsing performance
Memory + CPU usage
Extraction counts

➡ Stored in:
logs/performance.log
