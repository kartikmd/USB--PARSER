package com.myorg.usbparser.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.ZonedDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    private final ZonedDateTime timestamp;
    private final int status;
    private final String error;
    private final String message;
    private final String path;
}
