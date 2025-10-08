package com.myorg.usbparser.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Centralized exception handler for controller layer.
 * Returns JSON {@link ErrorResponse} and logs server-side details.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Helper: sanitize message (avoid returning null) and avoid exposing stack traces.
    private String safeMessage(String raw) {
        return (raw == null || raw.isBlank()) ? "No additional details" : raw;
    }

    // Helper: safe request URI
    private String safeUri(HttpServletRequest request) {
        try {
            return request == null ? "unknown" : Objects.toString(request.getRequestURI(), "unknown");
        } catch (Exception ex) {
            return "unknown";
        }
    }

    // Build response and also log the exception at appropriate level.
    private ResponseEntity<ErrorResponse> logAndBuild(Exception ex, String message, HttpStatus status, HttpServletRequest request) {
        String uri = safeUri(request);
        String safeMsg = safeMessage(message);
        ErrorResponse body = new ErrorResponse(LocalDateTime.now(), status.value(), status.getReasonPhrase(), safeMsg, uri);

        // Log at warn for client errors and error for server errors
        if (status.is4xxClientError()) {
            log.warn("Client error [{}] for {}: {} - {}", status.value(), uri, safeMsg, ex == null ? "" : ex.toString());
        } else {
            log.error("Server error [{}] for {}: {}", status.value(), uri, safeMsg, ex);
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body);
    }

    @ExceptionHandler(FileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFileNotFound(FileNotFoundException ex, HttpServletRequest request) {
        String msg = "File not found: " + safeMessage(ex == null ? null : ex.getMessage());
        return logAndBuild(ex, msg, HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ErrorResponse> handleIOException(IOException ex, HttpServletRequest request) {
        String msg = "I/O error: " + safeMessage(ex == null ? null : ex.getMessage());
        return logAndBuild(ex, msg, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingPart(MissingServletRequestPartException ex, HttpServletRequest request) {
        String part = ex == null ? null : ex.getRequestPartName();
        String msg = "Missing required part: " + safeMessage(part);
        return logAndBuild(ex, msg, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        String msg = "Uploaded file is too large!";
        return logAndBuild(ex, msg, HttpStatus.PAYLOAD_TOO_LARGE, request);
    }

    // Optional: handle your custom ValidationException separately for clearer messages.
    // Uncomment and replace 'com.myorg.usbparser.exception.ValidationException' with your real class if present.
    /*
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        String msg = safeMessage(ex == null ? null : ex.getMessage());
        return logAndBuild(ex, msg, HttpStatus.BAD_REQUEST, request);
    }
    */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        String msg = "Unexpected error occurred: " + safeMessage(ex == null ? null : ex.getMessage());
        return logAndBuild(ex, msg, HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
