# USB Power Delivery (USB-PD) PDF Parser

## 📌 Project Overview  
This project is a **Spring Boot application** that parses the official  
**USB Power Delivery Specification PDF** and generates structured output files.  
It automates extraction of documentation structure for testing, analysis, and content validation.

The system produces:

- 📄 **usb_pd_toc.jsonl** → Extracted Table of Contents  
- 📄 **usb_pd_sections.jsonl** → All extracted content sections  
- 📊 **validation_report.xlsx** → Comparison between TOC & actual parsed content  
- 🧾 **performance.log** → Execution time and processing metrics

---

## 🚀 Key Features

- Upload PDF via REST endpoint:  
  `POST /api/pdf/parse`
- Extracts **hierarchical Table of Contents**
- Extracts **all content sections with text**
- Generates a **validation report (Excel)**
- Includes detailed **performance + debugging logs**
- Global exception handling implemented for stability

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | **Java 17** |
| Framework | **Spring Boot 3.x** |
| Parsing Engine | **Apache PDFBox** |
| File Output | **Jackson JSONL Writer** |
| Validation | **Apache POI (Excel)** |
| Logging | **SLF4J + Logback** |
| Utility | **Lombok (models & builders)** |
| Testing | **JUnit 5 + Mockito** |

---

## ⚙️ Setup & Run

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/<your-username>/usb-pd-parser.git
cd usb-pd-parser
