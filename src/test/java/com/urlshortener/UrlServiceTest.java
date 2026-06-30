package com.urlshortener;

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
import com.urlshortener.service.UrlServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private ClickAnalyticRepository clickAnalyticRepository;

    @InjectMocks
    private UrlServiceImpl urlService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void testShortenUrl_WithCustomAlias_Success() {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://www.example.com")
                .customAlias("mycustom")
                .build();

        when(urlMappingRepository.existsByShortCode("mycustom")).thenReturn(false);
        when(urlMappingRepository.existsByCustomAlias("mycustom")).thenReturn(false);
        
        UrlMapping savedMapping = UrlMapping.builder()
                .id(1L)
                .originalUrl("https://www.example.com")
                .shortCode("mycustom")
                .customAlias("mycustom")
                .createdAt(LocalDateTime.now())
                .clickCount(0L)
                .build();

        when(urlMappingRepository.save(any(UrlMapping.class))).thenReturn(savedMapping);

        UrlResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("mycustom", response.getShortCode());
        assertEquals("http://localhost:8080/mycustom", response.getShortUrl());
        assertEquals("https://www.example.com", response.getOriginalUrl());
        verify(urlMappingRepository, times(1)).save(any(UrlMapping.class));
    }

    @Test
    void testShortenUrl_DuplicateAlias_ThrowsException() {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://www.example.com")
                .customAlias("taken")
                .build();

        when(urlMappingRepository.existsByShortCode("taken")).thenReturn(true);

        assertThrows(DuplicateAliasException.class, () -> urlService.shortenUrl(request));
        verify(urlMappingRepository, never()).save(any(UrlMapping.class));
    }

    @Test
    void testShortenUrl_ExistingUrl_ReturnsExisting() {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://www.google.com")
                .build();

        UrlMapping existingMapping = UrlMapping.builder()
                .id(2L)
                .originalUrl("https://www.google.com")
                .shortCode("goog01")
                .createdAt(LocalDateTime.now().minusDays(1))
                .clickCount(10L)
                .build();

        when(urlMappingRepository.findByOriginalUrl("https://www.google.com")).thenReturn(Optional.of(existingMapping));

        UrlResponse response = urlService.shortenUrl(request);

        assertNotNull(response);
        assertEquals("goog01", response.getShortCode());
        assertEquals("https://www.google.com", response.getOriginalUrl());
        verify(urlMappingRepository, never()).save(any(UrlMapping.class));
    }

    @Test
    void testResolveShortCode_Success() {
        UrlMapping mapping = UrlMapping.builder()
                .id(1L)
                .originalUrl("https://www.stackoverflow.com")
                .shortCode("stacky")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(urlMappingRepository.findByShortCode("stacky")).thenReturn(Optional.of(mapping));

        String resolved = urlService.resolveShortCode("stacky");

        assertEquals("https://www.stackoverflow.com", resolved);
        verify(urlMappingRepository, times(1)).incrementClickCount("stacky");
        verify(clickAnalyticRepository, times(1)).save(any(ClickAnalytic.class));
    }

    @Test
    void testResolveShortCode_Expired_ThrowsException() {
        UrlMapping mapping = UrlMapping.builder()
                .id(1L)
                .originalUrl("https://www.expired.com")
                .shortCode("exp123")
                .createdAt(LocalDateTime.now().minusDays(5))
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired yesterday
                .build();

        when(urlMappingRepository.findByShortCode("exp123")).thenReturn(Optional.of(mapping));

        assertThrows(UrlExpiredException.class, () -> urlService.resolveShortCode("exp123"));
        verify(urlMappingRepository, never()).incrementClickCount(anyString());
        verify(clickAnalyticRepository, never()).save(any(ClickAnalytic.class));
    }

    @Test
    void testResolveShortCode_NotFound_ThrowsException() {
        when(urlMappingRepository.findByShortCode("absent")).thenReturn(Optional.empty());

        assertThrows(UrlNotFoundException.class, () -> urlService.resolveShortCode("absent"));
    }

    @Test
    void testUpdateUrl_Success() {
        UrlMapping existing = UrlMapping.builder()
                .id(1L)
                .originalUrl("https://old.com")
                .shortCode("short1")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        when(urlMappingRepository.findByShortCode("short1")).thenReturn(Optional.of(existing));
        when(urlMappingRepository.save(any(UrlMapping.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://new.com")
                .expiryDays(5)
                .build();

        UrlResponse response = urlService.updateUrl("short1", request);

        assertEquals("https://new.com", response.getOriginalUrl());
        assertNotNull(response.getExpiresAt());
        verify(urlMappingRepository, times(1)).save(existing);
    }

    @Test
    void testGetStatsSummary() {
        when(urlMappingRepository.count()).thenReturn(10L);
        when(urlMappingRepository.sumAllClicks()).thenReturn(100L);
        when(urlMappingRepository.findFirstByOrderByClickCountDesc()).thenReturn(Optional.of(
                UrlMapping.builder().originalUrl("https://popular.com").shortCode("pop1").clickCount(50L).build()
        ));
        when(urlMappingRepository.findTop5ByOrderByCreatedAtDesc()).thenReturn(Collections.emptyList());
        when(clickAnalyticRepository.findHourlyClicksSince(any(LocalDateTime.class))).thenReturn(List.of(
                new Object[]{12, 10L},
                new Object[]{15, 20L}
        ));

        StatsResponse stats = urlService.getStatsSummary();

        assertEquals(10L, stats.getTotalUrls());
        assertEquals(100L, stats.getTotalClicks());
        assertEquals("pop1", stats.getMostClickedUrl().getShortCode());
        assertEquals(10L, stats.getHourlyClicks().get(12));
        assertEquals(20L, stats.getHourlyClicks().get(15));
        assertEquals(0L, stats.getHourlyClicks().get(0)); // check initialization
    }
}
