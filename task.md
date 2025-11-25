📄 USB Power Delivery (USB-PD) Parsing System – Task Documentation
📌 Project Overview
This project processes the official USB Power Delivery Specification PDF and automatically generates structured machine-readable outputs used for documentation analysis and validation.

The system extracts:

Table of Contents JSONL

Extracted PDF Sections JSONL

Excel Validation Report

Execution & performance logs

The implementation uses:

Java 17 · Spring Boot · Apache PDFBox · Jackson JSONL · Apache POI · SLF4J + Logback

🎯 Objectives
The goal of this system is to build a complete automated pipeline that:

1️⃣ Accepts and Stores a PDF
✔ Input received through a REST API (POST multipart upload)
✔ Validates file type and saves it to output/uploads/
✔ Logs upload timing, memory and CPU usage into performance.log

2️⃣ Extracts the Table of Contents (TOC)
✔ Detects various TOC formats:

Number-based (e.g., 1, 1.2, 3.4.5)

Annex sections (A, B, C …)

Dotted, spaced or trailing-page formats

✔ Extracted metadata includes:

Field	Meaning
section_id	The numeric/letter code (e.g., 1.2.3, A.1)
title	Cleaned TOC title text
page	Mapped PDF page number
level	Depth based on numbering
parent_id	Hierarchy reference
full_path	section_id + title
→ Stored as: usb_pd_toc.jsonl

3️⃣ Extracts Full Document Sections
✔ Extracts content from every page
✔ Detects section headings inside the document
✔ Assigns content to the corresponding TOC entry using:

Page tracking

Heading matching logic

Hierarchical rules

→ Stored as: usb_pd_sections.jsonl

4️⃣ Serializes Output to JSONL
✔ Uses Jackson streaming to produce line-by-line JSON objects
✔ Ensures lightweight memory usage even for large PDFs
✔ Generates two files:

usb_pd_toc.jsonl

usb_pd_sections.jsonl

5️⃣ Validates TOC vs Extracted Sections
Validation includes:

Missing sections

Extra sections

Page mismatch

Unmapped content

Ordering & hierarchy consistency

→ Results exported to validation_report.xlsx using Apache POI

6️⃣ Performance Monitoring & Logging
✔ Uses SLF4J + Logback for structured logging
✔ Uses PerfLogger for execution metrics:

Time taken for each stage

Memory usage

Section count

Error traces (if any)

→ Output: logs/performance.log

