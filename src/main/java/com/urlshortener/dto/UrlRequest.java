package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request payload to shorten a URL")
public class UrlRequest {

    @Schema(description = "The long URL to shorten", example = "https://www.example.com/very/long/url", required = true)
    @NotBlank(message = "Original URL cannot be blank")
    @URL(message = "Invalid URL format")
    private String originalUrl;

    @Schema(description = "Optional custom alias for the shortened URL", example = "mylink", required = false)
    private String customAlias;

    @Schema(description = "Optional number of days until the link expires", example = "7", required = false)
    @Min(value = 1, message = "Expiry days must be at least 1")
    private Integer expiryDays;
}
