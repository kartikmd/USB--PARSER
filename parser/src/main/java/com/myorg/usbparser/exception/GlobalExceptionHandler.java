package com.myorg.usbparser.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // --- Helpers ---
    private String safeMessage(String raw) {
        return (raw == null || raw.isBlank()) ? "No additional details" : raw;
    }

    private String safeUri(HttpServletRequest request) {
        try {
            return request == null ? "unknown" : Objects.toString(request.getRequestURI(), "unknown");
        } catch (Exception ex) {
            return "unknown";
        }
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(ZonedDateTime.now(ZoneId.of("UTC")))
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .build();
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    private ResponseEntity<ErrorResponse> logAndBuild(Exception ex, HttpStatus status,
                                                      String message, HttpServletRequest request) {
        String uri = safeUri(request);
        String msg = safeMessage(message);

        if (status.is4xxClientError()) {
            log.warn("Client error [{}] for {}: {} - {}", status.value(), uri, msg, ex == null ? "" : ex.toString());
        } else {
            log.error("Server error [{}] for {}: {}", status.value(), uri, msg, ex);
        }
        return build(status, msg, uri);
    }

    // --- Handlers ---
    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex, HttpServletRequest request) {
        return logAndBuild(ex, HttpStatus.NOT_FOUND, "File not found: " + safeMessage(ex.getMessage()), request);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex, HttpServletRequest request) {
        return logAndBuild(ex, HttpStatus.INTERNAL_SERVER_ERROR, "I/O error: " + safeMessage(ex.getMessage()), request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        return logAndBuild(ex, HttpStatus.BAD_REQUEST, "Missing required part: " + safeMessage(ex.getRequestPartName()), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return logAndBuild(ex, HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file is too large!", request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return logAndBuild(ex, HttpStatus.BAD_REQUEST, safeMessage(ex.getMessage()), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        return logAndBuild(ex, HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred: " + safeMessage(ex.getMessage()), request);
    }
}
