# System Architecture Diagram

```mermaid

graph TD

    %% Hardware
    subgraph Hardware
        Camera["📷 Camera
        Input: People standing in front
        Output: Live video (frames)"]

        Computer["💻 Computer
        Input: Video frames
        Output: Sends to Software"]
    end

    %% Software
    subgraph Software
        OpenCV["🖼️ OpenCV
        Input: Video frames
        Task: Find faces in each frame
        Output: Detected face images"]

        FaceRecognition["🙂 Face Recognition
        Input: Detected faces + Stored faces
        Task: Check if the face matches a known person
        Output: Person's Name / Unknown"]

        AttendanceManager["📒 Attendance Manager
        Input: Name / Unknown
        Task: Mark attendance for known people
        Output: Attendance record"]

        Logger["📝 Logger
        Input: System events & performance
        Task: Record errors, steps, CPU & memory usage
        Output: Log messages + performance details"]
    end

    %% Storage
    subgraph Storage
        FaceDB["🗂️ Face Database
        Stored: Photos of employees"]

        CSV["📂 Attendance.csv
        Stored: Name, ID, Date, Time, Status"]

        LogFile["📄 performance.log
        Stored: Events, Errors,
        CPU & Memory usage,
        Processing time"]
    end

    %% Connections
    Camera --> Computer
    Computer --> OpenCV
    OpenCV --> FaceRecognition
    FaceRecognition --> AttendanceManager
    FaceRecognition -->|Compare with| FaceDB
    AttendanceManager --> CSV
    AttendanceManager --> Logger
    Logger --> LogFile
