# USB Power Delivery (USB-PD) PDF Parser

## 📌 Project Overview
This project is a **Spring Boot application** that parses the official **USB Power Delivery Specification PDF**.  
It extracts:
- The **Table of Contents (ToC)** into one JSONL file.
- All **other sections** into a second JSONL file.
- A **validation report (Excel)** comparing ToC sections vs parsed sections (with missing/extra counts).

It follows:
- ✅ 100% OOP principles
- ✅ Java 17 + Spring Boot 3.x
- ✅ Lombok (for boilerplate reduction)
- ✅ Proper Java naming conventions
- ✅ Global exception handling

---

## 🚀 Features
- **Upload PDF** via REST API (`/api/pdf/parse`)
- Extracts **ToC** into `usb_pd_toc.jsonl`
- Extracts **all sections** into `usb_pd_sections.jsonl`
- Generates **Excel validation report** (`validation_report.xlsx`)
- Handles exceptions globally (`GlobalExceptionHandler`)
- Supports **JUnit/Mockito tests** for services, controller, and validation logic

---

## 🛠️ Tech Stack
- **Java 17**
- **Spring Boot 3.2.x**
- **Apache PDFBox** → for PDF parsing
- **Jackson** → for JSONL writing
- **Apache POI** → for Excel validation reports
- **Lombok** → for cleaner models
- **JUnit 5 + Mockito** → for unit/integration tests

---

## ⚙️ Setup & Run

### 1. Clone the Repository
```bash
git clone https://github.com/<your-username>/usb-pd-parser.git
cd usb-pd-parser
