package com.urlshortener.dto;

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
@Schema(description = "Response details of a shortened URL")
public class UrlResponse {

    @Schema(description = "The original long URL", example = "https://www.example.com/very/long/url")
    private String originalUrl;

    @Schema(description = "The fully qualified shortened URL", example = "http://localhost:8080/mylink")
    private String shortUrl;

    @Schema(description = "The unique 6-character short code", example = "mylink")
    private String shortCode;

    @Schema(description = "The timestamp when the mapping was created", example = "2026-06-15T15:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "The expiration timestamp (null if it never expires)", example = "2026-06-22T15:00:00")
    private LocalDateTime expiresAt;

    @Schema(description = "The total number of redirection clicks recorded", example = "42")
    private Long clickCount;

    @Schema(description = "The custom alias supplied during creation (null if auto-generated)", example = "mylink")
    private String customAlias;
}
