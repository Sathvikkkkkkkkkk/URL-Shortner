package com.urlshortener.controller;

import com.urlshortener.dto.StatsResponse;
import com.urlshortener.dto.UrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.service.UrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Tag(name = "URL Shortener API", description = "Endpoints for creating, managing, and retrieving statistics for shortened URLs")
public class UrlController {

    private final UrlService urlService;

    // Constructor injection
    public UrlController(UrlService urlService) {
        this.urlService = urlService;
    }

    @PostMapping(value = "/shorten", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a shortened URL", description = "Shortens a given long URL. Supports custom alias and link expiration in days.")
    @ApiResponse(responseCode = "201", description = "URL successfully shortened", 
            content = @Content(schema = @Schema(implementation = UrlResponse.class)))
    @ApiResponse(responseCode = "400", description = "Invalid URL format or request parameters")
    @ApiResponse(responseCode = "409", description = "Custom alias is already in use")
    public ResponseEntity<UrlResponse> shortenUrl(@Valid @RequestBody UrlRequest request) {
        UrlResponse response = urlService.shortenUrl(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping(value = "/info/{shortCode}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get URL information", description = "Fetches the metadata and click stats of a shortened URL by its short code.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved mapping details",
            content = @Content(schema = @Schema(implementation = UrlResponse.class)))
    @ApiResponse(responseCode = "404", description = "Short code not found")
    public ResponseEntity<UrlResponse> getUrlInfo(
            @Parameter(description = "The 6-character short code or custom alias", required = true)
            @PathVariable String shortCode) {
        UrlResponse response = urlService.getUrlInfo(shortCode);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/urls", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all shortened URLs", description = "Retrieves a paginated list of all shortened URLs in the system.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved page of URLs")
    public ResponseEntity<Page<UrlResponse>> listAllUrls(
            @PageableDefault(page = 0, size = 10) Pageable pageable) {
        Page<UrlResponse> response = urlService.listAllUrls(pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/urls/{shortCode}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update a shortened URL", description = "Updates the target original URL or extends the expiration duration of a mapping.")
    @ApiResponse(responseCode = "200", description = "Successfully updated mapping details",
            content = @Content(schema = @Schema(implementation = UrlResponse.class)))
    @ApiResponse(responseCode = "404", description = "Short code not found")
    @ApiResponse(responseCode = "400", description = "Invalid request arguments")
    public ResponseEntity<UrlResponse> updateUrl(
            @Parameter(description = "The short code to update", required = true)
            @PathVariable String shortCode,
            @Valid @RequestBody UrlRequest request) {
        UrlResponse response = urlService.updateUrl(shortCode, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/urls/{shortCode}")
    @Operation(summary = "Delete a shortened URL mapping", description = "Deletes a URL mapping by its short code. Returns 204 No Content.")
    @ApiResponse(responseCode = "24", description = "URL mapping successfully deleted") // wait, 204
    @ApiResponse(responseCode = "204", description = "URL mapping successfully deleted")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    public ResponseEntity<Void> deleteUrl(
            @Parameter(description = "The short code to delete", required = true)
            @PathVariable String shortCode) {
        urlService.deleteUrl(shortCode);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get service stats summary", description = "Returns system-wide diagnostics: total shortened links, aggregate click counts, the most clicked URL, and recent history.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved system stats",
            content = @Content(schema = @Schema(implementation = StatsResponse.class)))
    public ResponseEntity<StatsResponse> getStatsSummary() {
        StatsResponse response = urlService.getStatsSummary();
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/urls/{shortCode}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    @Operation(summary = "Generate a QR code image", description = "Generates and returns a PNG QR code image that redirects to the shortened URL.")
    @ApiResponse(responseCode = "200", description = "Successfully generated QR code image")
    @ApiResponse(responseCode = "404", description = "Short code not found")
    public ResponseEntity<byte[]> getQrCode(
            @Parameter(description = "The short code to generate QR code for", required = true)
            @PathVariable String shortCode) {
        byte[] qrCodeBytes = urlService.generateQrCode(shortCode);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);
        headers.setCacheControl("public, max-age=86400"); // Cache QR code for 1 day
        return new ResponseEntity<>(qrCodeBytes, headers, HttpStatus.OK);
    }
}
