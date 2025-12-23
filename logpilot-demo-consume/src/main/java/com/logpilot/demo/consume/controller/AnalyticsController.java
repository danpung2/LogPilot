package com.logpilot.demo.consume.controller;

import com.logpilot.demo.consume.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/analytics/stats")
    public Map<String, Object> getStats() {
        return analyticsService.getStats();
    }
}
