package com.urlshortener.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Standard API error response payload")
public class ErrorResponse {

    @Schema(description = "The timestamp when the error occurred", example = "2026-06-15T15:10:00")
    private LocalDateTime timestamp;

    @Schema(description = "The HTTP status code", example = "404")
    private int status;

    @Schema(description = "The short HTTP status error name", example = "Not Found")
    private String error;

    @Schema(description = "Detailed human-readable error message", example = "Short URL 'abc123' not found")
    private String message;

    @Schema(description = "The request path that triggered the error", example = "/abc123")
    private String path;
}
