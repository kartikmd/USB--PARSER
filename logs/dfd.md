```mermaid
flowchart TD
  User["👤 User / Dev"]
  System["📦 USB PD Parser"]

  User -->|upload/run| System

  subgraph System
    Ingest["1 - Ingest PDF"]
    Parse["2 - Parse ToC & Sections"]
    Post["3 - Post-process & Write JSONL"]
    Validate["4 - Validate & Report"]
  end

  Ingest --> Parse
  Parse --> Post
  Post --> Validate

  Post -->|"usb_pd_toc.jsonl\nusb_pd_sections.jsonl"| Storage["🗂 Outputs"]
  Validate -->|"validation_report.xlsx"| Storage
  Storage -->|download| User

