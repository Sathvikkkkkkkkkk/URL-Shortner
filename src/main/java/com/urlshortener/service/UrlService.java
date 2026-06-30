package com.urlshortener.service;

import com.urlshortener.dto.StatsResponse;
import com.urlshortener.dto.UrlRequest;
import com.urlshortener.dto.UrlResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UrlService {
    
    /**
     * Shorten a URL mapping based on the request.
     */
    UrlResponse shortenUrl(UrlRequest request);

    /**
     * Resolve a short code to its original URL, incrementing metrics atomically.
     */
    String resolveShortCode(String shortCode);

    /**
     * Retrieve the full info for a specific short code.
     */
    UrlResponse getUrlInfo(String shortCode);

    /**
     * Paginated list of all URLs.
     */
    Page<UrlResponse> listAllUrls(Pageable pageable);

    /**
     * Delete a URL mapping.
     */
    void deleteUrl(String shortCode);

    /**
     * Update fields of a URL mapping (original URL or expiration date).
     */
    UrlResponse updateUrl(String shortCode, UrlRequest request);

    /**
     * Retrieve summary statistics for the service dashboard.
     */
    StatsResponse getStatsSummary();

    /**
     * Generate a QR code for the shortened URL.
     */
    byte[] generateQrCode(String shortCode);
}
