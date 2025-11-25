# USB Power Delivery (USB-PD) Parsing – Task Documentation

## 📌 Project Overview

This project reads the official **USB Power Delivery Specification PDF** and
generates three main outputs:

- `usb_pd_toc.jsonl` – Table of Contents entries  
- `usb_pd_sections.jsonl` – Parsed section content with pages  
- `validation_report.xlsx` – Excel report comparing TOC vs sections  

All of this is exposed as a **Spring Boot REST API** that accepts a PDF upload
and writes results to the `output/` folder. Performance details are written to
`logs/performance.log`.

Tech stack: **Java 17, Spring Boot, Apache PDFBox, Jackson, Apache POI, SLF4J / Logback**.

---

## 🎯 High-Level Objectives

### 1. Accept the USB-PD PDF (REST API)

- Expose `POST /api/pdf/parse` in `PdfParserController`.
- Accept a multipart **PDF file** from the client.
- Validate:
  - file is not empty  
  - file type is PDF  
- Save the uploaded file under the configured `output/` directory.

### 2. Extract Table of Contents (TOC)

- Use `PdfBoxTocExtractor` (Apache PDFBox) to scan the TOC pages.
- Detect entries such as:
  - Numbered sections: `1`, `1.1`, `10.3.1`
  - Front-matter: `Revision History`, `Editors`, `Contributors`
  - Annex headings: `A CRC calculation`, `B Message Sequence Examples`, etc.
- Build TOC records with:
  - `sectionId` (when available)  
  - `title`  
  - `page`  
  - hierarchy info (level, parentId, fullPath)
- Later written to `usb_pd_toc.jsonl`.

### 3. Extract Sections (Content + Pages)

- Use `PdfBoxSectionExtractor` to read **all pages** of the PDF.
- Detect section headings (e.g. `1 Introduction`, `1.2 Purpose`, `C.1 Discover Identity Example`).
- Attach the body text under each heading:
  - collect lines until the next heading  
  - ignore headers/footers and layout noise
- Build a list of `Section` objects containing:
  - doc title  
  - section id  
  - title  
  - page  
  - hierarchy (level, parentId, fullPath)  
  - `content` (cleaned text)

### 4. Write JSONL Outputs

- Use `JsonlWriter` (Jackson based) to serialize:
  - TOC list → `usb_pd_toc.jsonl`  
  - Sections list → `usb_pd_sections.jsonl`
- Each line is **one JSON object**, so files are:
  - easy to stream  
  - easy to process with scripts/tools later.

### 5. Validate TOC vs Sections (Excel Report)

- Use `ExcelValidator` (Apache POI) to compare:
  - entries from `usb_pd_toc.jsonl`
  - entries from `usb_pd_sections.jsonl`
- Check for:
  - TOC entries that have no parsed section  
  - parsed sections that are not in TOC  
  - page mismatches and basic structure issues
- Write a human-readable Excel file:
  - `validation_report.xlsx` in the `output/` folder.

### 6. Logging & Performance

- Use **SLF4J + Logback** with a dedicated `performance` logger.
- Log important steps with timing, CPU and memory usage, for example:
  - upload saved  
  - TOC extracted  
  - sections extracted  
  - JSONL written  
  - validation report written  
  - job complete summary
- Logs are written to:
  - `logs/performance.log`

---

## ✅ Final Outputs

After a successful run for a given PDF, the project produces:

- `output/usb_pd_toc.jsonl`  
- `output/usb_pd_sections.jsonl`  
- `output/validation_report.xlsx`  
- `logs/performance.log`
