package com.urlshortener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.controller.RedirectController;
import com.urlshortener.controller.UrlController;
import com.urlshortener.dto.StatsResponse;
import com.urlshortener.dto.UrlRequest;
import com.urlshortener.dto.UrlResponse;
import com.urlshortener.exception.*;
import com.urlshortener.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {UrlController.class, RedirectController.class}, properties = "app.rate-limiting.enabled=false")
@Import(GlobalExceptionHandler.class)
class UrlControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlService urlService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testShortenUrl_Success() throws Exception {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://www.example.com")
                .customAlias("myalias")
                .expiryDays(7)
                .build();

        UrlResponse response = UrlResponse.builder()
                .originalUrl("https://www.example.com")
                .shortCode("myalias")
                .shortUrl("http://localhost:8080/myalias")
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(7))
                .clickCount(0L)
                .customAlias("myalias")
                .build();

        when(urlService.shortenUrl(any(UrlRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("myalias"))
                .andExpect(jsonPath("$.originalUrl").value("https://www.example.com"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/myalias"));

        verify(urlService, times(1)).shortenUrl(any(UrlRequest.class));
    }

    @Test
    void testShortenUrl_ValidationError() throws Exception {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("invalid-url-format") // invalid URL annotation
                .build();

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());

        verify(urlService, never()).shortenUrl(any(UrlRequest.class));
    }

    @Test
    void testShortenUrl_DuplicateAliasConflict() throws Exception {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://example.com")
                .customAlias("alreadytaken")
                .build();

        when(urlService.shortenUrl(any(UrlRequest.class)))
                .thenThrow(new DuplicateAliasException("Custom alias 'alreadytaken' is already in use"));

        mockMvc.perform(post("/api/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Custom alias 'alreadytaken' is already in use"));
    }

    @Test
    void testRedirect_Success() throws Exception {
        when(urlService.resolveShortCode("abc123")).thenReturn("https://www.google.com");

        mockMvc.perform(get("/abc123"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://www.google.com"));

        verify(urlService, times(1)).resolveShortCode("abc123");
    }

    @Test
    void testRedirect_Expired() throws Exception {
        when(urlService.resolveShortCode("exp123")).thenThrow(new UrlExpiredException("Link has expired"));

        mockMvc.perform(get("/exp123"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error").value("Gone"))
                .andExpect(jsonPath("$.message").value("Link has expired"));
    }

    @Test
    void testRedirect_NotFound() throws Exception {
        when(urlService.resolveShortCode("missing")).thenThrow(new UrlNotFoundException("Short URL 'missing' not found"));

        mockMvc.perform(get("/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Short URL 'missing' not found"));
    }

    @Test
    void testGetUrlInfo_Success() throws Exception {
        UrlResponse response = UrlResponse.builder()
                .originalUrl("https://example.com")
                .shortCode("info12")
                .shortUrl("http://localhost:8080/info12")
                .createdAt(LocalDateTime.now())
                .clickCount(5L)
                .build();

        when(urlService.getUrlInfo("info12")).thenReturn(response);

        mockMvc.perform(get("/api/info/info12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("info12"))
                .andExpect(jsonPath("$.clickCount").value(5));
    }

    @Test
    void testListAllUrls() throws Exception {
        when(urlService.listAllUrls(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        mockMvc.perform(get("/api/urls")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void testUpdateUrl_Success() throws Exception {
        UrlRequest request = UrlRequest.builder()
                .originalUrl("https://newtarget.com")
                .expiryDays(2)
                .build();

        UrlResponse response = UrlResponse.builder()
                .originalUrl("https://newtarget.com")
                .shortCode("up123")
                .expiresAt(LocalDateTime.now().plusDays(2))
                .build();

        when(urlService.updateUrl(eq("up123"), any(UrlRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/urls/up123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value("https://newtarget.com"));
    }

    @Test
    void testDeleteUrl_Success() throws Exception {
        doNothing().when(urlService).deleteUrl("del123");

        mockMvc.perform(delete("/api/urls/del123"))
                .andExpect(status().isNoContent());

        verify(urlService, times(1)).deleteUrl("del123");
    }

    @Test
    void testGetStatsSummary_Success() throws Exception {
        StatsResponse stats = StatsResponse.builder()
                .totalUrls(50L)
                .totalClicks(5000L)
                .hourlyClicks(new HashMap<>())
                .build();

        when(urlService.getStatsSummary()).thenReturn(stats);

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUrls").value(50))
                .andExpect(jsonPath("$.totalClicks").value(5000));
    }

    @Test
    void testGetQrCode_Success() throws Exception {
        byte[] qrBytes = new byte[]{1, 2, 3, 4};
        when(urlService.generateQrCode("qr123")).thenReturn(qrBytes);

        mockMvc.perform(get("/api/urls/qr123/qrcode"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG_VALUE))
                .andExpect(content().bytes(qrBytes));
    }
}
