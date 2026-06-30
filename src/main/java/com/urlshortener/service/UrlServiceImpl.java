package com.urlshortener.service;

import com.urlshortener.dto.StatsResponse;
import com.urlshortener.dto.UrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.DuplicateAliasException;
import com.urlshortener.exception.UrlExpiredException;
import com.urlshortener.exception.UrlNotFoundException;
import com.urlshortener.model.ClickAnalytic;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.ClickAnalyticRepository;
import com.urlshortener.repository.UrlMappingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UrlServiceImpl implements UrlService {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickAnalyticRepository clickAnalyticRepository;
    
    @Value("${app.base-url}")
    private String baseUrl;

    // Constructor injection
    public UrlServiceImpl(UrlMappingRepository urlMappingRepository, ClickAnalyticRepository clickAnalyticRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.clickAnalyticRepository = clickAnalyticRepository;
    }

    @Override
    @Transactional
    public UrlResponse shortenUrl(UrlRequest request) {
        // 1. Check if originalUrl is already shortened and active (not expired)
        Optional<UrlMapping> existing = urlMappingRepository.findByOriginalUrl(request.getOriginalUrl());
        if (existing.isPresent()) {
            UrlMapping urlMapping = existing.get();
            if (urlMapping.getExpiresAt() == null || urlMapping.getExpiresAt().isAfter(LocalDateTime.now())) {
                return mapToResponse(urlMapping);
            }
        }

        String shortCode;
        String customAlias = request.getCustomAlias();

        // 2. Validate custom alias if provided, or generate short code
        if (customAlias != null && !customAlias.trim().isEmpty()) {
            String trimmedAlias = customAlias.trim();
            if (urlMappingRepository.existsByShortCode(trimmedAlias) || urlMappingRepository.existsByCustomAlias(trimmedAlias)) {
                throw new DuplicateAliasException("Custom alias '" + trimmedAlias + "' is already in use");
            }
            shortCode = trimmedAlias;
        } else {
            long timestamp = System.currentTimeMillis();
            int salt = 0;
            shortCode = Base62Encoder.encode(request.getOriginalUrl(), timestamp, salt);
            
            // Collision detection loop
            while (urlMappingRepository.existsByShortCode(shortCode)) {
                salt++;
                shortCode = Base62Encoder.encode(request.getOriginalUrl(), timestamp, salt);
            }
        }

        // 3. Set properties and expiration
        LocalDateTime expiresAt = null;
        if (request.getExpiryDays() != null) {
            expiresAt = LocalDateTime.now().plusDays(request.getExpiryDays());
        }

        UrlMapping urlMapping = UrlMapping.builder()
                .originalUrl(request.getOriginalUrl())
                .shortCode(shortCode)
                .customAlias(customAlias != null && !customAlias.trim().isEmpty() ? customAlias.trim() : null)
                .expiresAt(expiresAt)
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .build();

        UrlMapping saved = urlMappingRepository.save(urlMapping);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public String resolveShortCode(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL '" + shortCode + "' not found"));

        // Check expiration
        if (urlMapping.getExpiresAt() != null && urlMapping.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("Link has expired");
        }

        // Increment clickCount atomically in the database
        urlMappingRepository.incrementClickCount(shortCode);

        // Record click analytic log
        ClickAnalytic clickAnalytic = ClickAnalytic.builder()
                .urlMapping(urlMapping)
                .clickedAt(LocalDateTime.now())
                .build();
        clickAnalyticRepository.save(clickAnalytic);

        return urlMapping.getOriginalUrl();
    }

    @Override
    @Transactional(readOnly = true)
    public UrlResponse getUrlInfo(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL '" + shortCode + "' not found"));
        return mapToResponse(urlMapping);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UrlResponse> listAllUrls(Pageable pageable) {
        return urlMappingRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void deleteUrl(String shortCode) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL '" + shortCode + "' not found"));
        urlMappingRepository.delete(urlMapping);
    }

    @Override
    @Transactional
    public UrlResponse updateUrl(String shortCode, UrlRequest request) {
        UrlMapping urlMapping = urlMappingRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new UrlNotFoundException("Short URL '" + shortCode + "' not found"));

        if (request.getOriginalUrl() != null && !request.getOriginalUrl().isBlank()) {
            urlMapping.setOriginalUrl(request.getOriginalUrl());
        }

        if (request.getExpiryDays() != null) {
            urlMapping.setExpiresAt(LocalDateTime.now().plusDays(request.getExpiryDays()));
        }

        UrlMapping saved = urlMappingRepository.save(urlMapping);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StatsResponse getStatsSummary() {
        long totalUrls = urlMappingRepository.count();
        long totalClicks = urlMappingRepository.sumAllClicks();
        
        Optional<UrlMapping> mostClickedOpt = urlMappingRepository.findFirstByOrderByClickCountDesc();
        UrlResponse mostClickedUrl = mostClickedOpt.map(this::mapToResponse).orElse(null);
        
        List<UrlMapping> recentUrls = urlMappingRepository.findTop5ByOrderByCreatedAtDesc();
        List<UrlResponse> recentlyCreated = recentUrls.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        // Construct hourly click statistics for the last 24 hours
        LocalDateTime since24HoursAgo = LocalDateTime.now().minusDays(1);
        List<Object[]> rawStats = clickAnalyticRepository.findHourlyClicksSince(since24HoursAgo);
        
        Map<Integer, Long> hourlyClicks = new HashMap<>();
        for (int h = 0; h < 24; h++) {
            hourlyClicks.put(h, 0L);
        }
        for (Object[] row : rawStats) {
            if (row[0] != null && row[1] != null) {
                Integer hour = (Integer) row[0];
                Long count = (Long) row[1];
                hourlyClicks.put(hour, count);
            }
        }

        return StatsResponse.builder()
                .totalUrls(totalUrls)
                .totalClicks(totalClicks)
                .mostClickedUrl(mostClickedUrl)
                .recentlyCreated(recentlyCreated)
                .hourlyClicks(hourlyClicks)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateQrCode(String shortCode) {
        // Validate shortCode exists
        if (!urlMappingRepository.existsByShortCode(shortCode)) {
            throw new UrlNotFoundException("Short URL '" + shortCode + "' not found");
        }

        String shortUrl = baseUrl + "/" + shortCode;
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(shortUrl, com.google.zxing.BarcodeFormat.QR_CODE, 300, 300);
            
            ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            return pngOutputStream.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate QR code for URL: " + shortUrl, e);
        }
    }

    private UrlResponse mapToResponse(UrlMapping mapping) {
        return UrlResponse.builder()
                .originalUrl(mapping.getOriginalUrl())
                .shortCode(mapping.getShortCode())
                .shortUrl(baseUrl + "/" + mapping.getShortCode())
                .createdAt(mapping.getCreatedAt())
                .expiresAt(mapping.getExpiresAt())
                .clickCount(mapping.getClickCount())
                .customAlias(mapping.getCustomAlias())
                .build();
    }
}
