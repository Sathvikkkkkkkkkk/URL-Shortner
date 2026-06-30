package com.urlshortener.controller;

import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Tag(name = "Redirection API", description = "Base endpoint used to resolve short codes and redirect to target locations")
public class RedirectController {

    private final UrlService urlService;

    // Constructor injection
    public RedirectController(UrlService urlService) {
        this.urlService = urlService;
    }

    @GetMapping("/{shortCode:[a-zA-Z0-9_-]+}")
    @Operation(summary = "Redirect to original URL", description = "Resolves the 6-character short code or custom alias, increments the click counter, registers a click timestamp, and redirects the client using an HTTP 302 response.")
    @ApiResponse(responseCode = "302", description = "Found - Redirecting to the original target URL")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    @ApiResponse(responseCode = "410", description = "Link has expired")
    public ResponseEntity<Void> redirect(
            @Parameter(description = "The short code or custom alias to resolve", required = true)
            @PathVariable String shortCode) {
        String originalUrl = urlService.resolveShortCode(shortCode);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originalUrl));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
