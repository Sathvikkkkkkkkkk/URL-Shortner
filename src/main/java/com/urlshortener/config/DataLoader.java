package com.urlshortener.config;

import com.urlshortener.model.ClickAnalytic;
import com.urlshortener.model.UrlMapping;
import com.urlshortener.repository.ClickAnalyticRepository;
import com.urlshortener.repository.UrlMappingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataLoader implements CommandLineRunner {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickAnalyticRepository clickAnalyticRepository;

    public DataLoader(UrlMappingRepository urlMappingRepository, ClickAnalyticRepository clickAnalyticRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.clickAnalyticRepository = clickAnalyticRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // 1. A standard URL with no expiration and 100 clicks
        UrlMapping url1 = UrlMapping.builder()
                .originalUrl("https://www.google.com")
                .shortCode("goog01")
                .createdAt(LocalDateTime.now().minusDays(10))
                .clickCount(100L)
                .build();

        // 2. A URL expiring in the future (7 days from now)
        UrlMapping url2 = UrlMapping.builder()
                .originalUrl("https://www.github.com")
                .shortCode("git002")
                .createdAt(LocalDateTime.now().minusDays(2))
                .expiresAt(LocalDateTime.now().plusDays(5))
                .clickCount(25L)
                .build();

        // 3. An already expired URL (expired 3 days ago)
        UrlMapping url3 = UrlMapping.builder()
                .originalUrl("https://stackoverflow.com")
                .shortCode("stack3")
                .createdAt(LocalDateTime.now().minusDays(10))
                .expiresAt(LocalDateTime.now().minusDays(3))
                .clickCount(5L)
                .build();

        // 4. A URL with custom alias
        UrlMapping url4 = UrlMapping.builder()
                .originalUrl("https://www.reddit.com")
                .shortCode("reddit")
                .customAlias("reddit")
                .createdAt(LocalDateTime.now().minusDays(4))
                .clickCount(350L)
                .build();

        // 5. A heavily clicked URL for analytics testing
        UrlMapping url5 = UrlMapping.builder()
                .originalUrl("https://en.wikipedia.org")
                .shortCode("wiki05")
                .createdAt(LocalDateTime.now().minusDays(1))
                .clickCount(1000L)
                .build();

        urlMappingRepository.saveAll(List.of(url1, url2, url3, url4, url5));

        // Create sample click analytics distribution over different hours
        LocalDateTime baseTime = LocalDateTime.now().minusHours(12);
        for (int i = 0; i < 15; i++) {
            // Click at hour: baseTime + i hours
            LocalDateTime clickTime = baseTime.plusHours(i);
            clickAnalyticRepository.save(ClickAnalytic.builder()
                    .urlMapping(url5)
                    .clickedAt(clickTime)
                    .build());
        }
        for (int i = 0; i < 5; i++) {
            clickAnalyticRepository.save(ClickAnalytic.builder()
                    .urlMapping(url4)
                    .clickedAt(LocalDateTime.now().minusMinutes(30))
                    .build());
        }
        
        System.out.println("--- URL Shortener Sample Data Preloaded Successfully ---");
    }
}
