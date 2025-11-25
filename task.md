USB Power Delivery (USB-PD) Parsing Assignment – Task Documentation

📌 Project Overview  
This project processes the USB Power Delivery Specification PDF and extracts structured data required for automated compliance validation and technical documentation.

The system generates the following outputs:-
    Output	                         Description
usb_pd_toc.jsonl	                 Extracted Table of Contents (structured format)
usb_pd_sections.jsonl	            Parsed full content sections mapped to headings
validation_report.xlsx	           Comparison report between TOC and extracted sections
performance.log	                  Processing time, execution metrics, and errors (if any)

The solution is built using:

Java 17

Spring Boot

Apache PDFBox

Apache POI

Jackson JSONL Streaming

SLF4J + Logback Logging
