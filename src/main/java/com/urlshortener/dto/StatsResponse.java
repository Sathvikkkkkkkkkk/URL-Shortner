package com.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Statistics summary of the URL Shortener service")
public class StatsResponse {

    @Schema(description = "Total number of shortened URLs in the system", example = "42")
    private Long totalUrls;

    @Schema(description = "Sum of click counts across all shortened URLs", example = "1500")
    private Long totalClicks;

    @Schema(description = "Details of the most clicked URL mapping")
    private UrlResponse mostClickedUrl;

    @Schema(description = "List of the most recently created URL mappings")
    private List<UrlResponse> recentlyCreated;

    @Schema(description = "Breakdown of click counts by hour of the day (0-23) for analytics", example = "{\"12\": 150, \"13\": 320}")
    private Map<Integer, Long> hourlyClicks;
}
