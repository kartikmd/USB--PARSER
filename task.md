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
📁 Output Directory Structure
output/
 ├── uploads/                       (uploaded PDF)
 ├── usb_pd_toc.jsonl
 ├── usb_pd_sections.jsonl
 ├── validation_report.xlsx
logs/
 └── performance.log
🧪 Example Log Entries
Upload saved: 156 ms, CPU: 7%, Memory: 67 MB
ToC extracted: 3992 ms, items=265
Sections extracted: 16038 ms, items=4773
JSONL written: 132 ms, total=5038
Validation report written: 1698 ms
Job complete: 24826 ms, CPU: 21%, Memory: 633 MB
🧠 Key Features
Feature	Description
📑 PDF Parsing	Handles complex formatting, dotted leaders, annexes, printed page numbers
🧼 Cleaning	Fixes broken TOC lines and normalizes content
🔁 Deduplication	Removes duplicate section entries based on best quality
📊 Validation	Ensures TOC & extracted content are aligned
⚡ Performance monitoring	Logs execution efficiency and resource usage
