# USB Power Delivery (USB-PD) Parsing Assignment – Task Documentation

## 📌 Project Overview

This project reads the official **USB Power Delivery Specification (Rev 3.2, V1.1)** PDF and produces:

- **Table of Contents JSONL** – structured TOC entries
- **Parsed Sections JSONL** – full section content with page mapping
- **Excel Validation Report** – compares TOC vs parsed sections
- **Performance Logs & Design Docs** – performance.log, architecture.md, dfd.md, sequence.md

The system is built with **Java 17**, **Spring Boot**, **Apache PDFBox**, **Jackson**, **Apache POI**, and **SLF4J + Logback**.

---

## 🎯 High-Level Objectives

The assignment was to build an **end-to-end PDF parsing pipeline** that:

1. **Accepts the USB-PD PDF via REST API**
   - Expose `POST /api/pdf/parse` in `PdfParserController`.
   - Accept a multipart **PDF file** from client / Postman.
   - Validate that:
     - File is present and non-empty.
     - File has a `.pdf` extension or `contentType` containing `pdf`.
   - Save the uploaded file under the configured **`output/`** folder.

2. **Extracts Table of Contents (TOC) using PDFBox**
   - Use **`PdfBoxTocExtractor`** (Apache PDFBox + regex) to read TOC pages.
   - Support:
     - Numbered headings → `1`, `1.2`, `3.4.5`, etc.
     - Front-matter headings → `Revision History`, `Editors`, `Contributors`, etc.
     - Annex / appendix style headings → `A`, `B`, `C` (e.g., “A CRC calculation”).
   - Build structured TOC entries as `Section` objects with:
     - `doc_title`
     - `section_id` (for numbered sections)
     - `title`
     - `page`
     - `level`
     - `parent_id`
     - `full_path`
   - **Output file:** `usb_pd_toc.jsonl` (one JSON object per line).

3. **Extracts Full Sections and Content using PDFBox**
   - Use **`PdfBoxSectionExtractor`** to scan the entire document.
   - Detect headings like `1 Introduction`, `1.2 Purpose`, etc.
   - For each section:
     - Track **printed page number** when available, otherwise PDF page index.
     - Capture **section content text** (skipping headers/footers like “Universal Serial Bus Power Delivery Specification”, “Page N”).
   - Represent each section as a `Section` model with:
     - metadata (id, title, level, parent, page)
     - `content` (body text)
   - **Output file:** `usb_pd_sections.jsonl`.

4. **Serializes TOC & Sections as JSONL (Jackson)**
   - Use a generic **`JsonlWriter<Section>`** implemented by **`JacksonJsonlWriter`**.
   - Responsibilities:
     - Stream over the list of `Section` objects.
     - Serialize each object as **one JSON line** (JSONL format).
     - Write to disk with UTF-8 encoding.
   - Files written under `output/`:
     - `usb_pd_toc.jsonl`
     - `usb_pd_sections.jsonl`

5. **Validates TOC vs Sections into an Excel Report**
   - Use **`ExcelValidator`** (implements `Validator`) with **Apache POI**.
   - Compare:
     - TOC entries (`usb_pd_toc.jsonl` in memory)  
       **vs**  
       Parsed sections (`usb_pd_sections.jsonl` in memory).
   - Detect and record:
     - **Missing sections** – present in TOC, not found in parsed sections.
     - **Extra sections** – parsed, but not listed in TOC.
     - **Page mismatches** – same section id/title, different page numbers.
     - **Structure issues** – inconsistent levels / hierarchy.
   - Generate a detailed **Excel report** summarizing these checks.
   - **Output file:** `validation_report.xlsx` in `output/`.

6. **Logs Performance & Execution Details**
   - Use **SLF4J + Logback** for application and performance logging.
   - A dedicated `PerfLogger` writes high-level metrics for each step:
     - upload time
     - TOC extraction duration and item count
     - section extraction duration and item count
     - JSONL writing time and total records
     - validation time
     - overall job time, CPU usage, memory usage
   - All performance entries are stored in:
     - **`logs/performance.log`**

---

## 📂 Key Outputs & Deliverables

- `output/usb_pd_toc.jsonl` – parsed Table of Contents.
- `output/usb_pd_sections.jsonl` – parsed sections with content.
- `output/validation_report.xlsx` – Excel validation report.
- `logs/performance.log` – performance & timing logs.
- `architecture.md`, `dfd.md`, `sequence.md`, `README.md`, `task.md` – project documentation.
