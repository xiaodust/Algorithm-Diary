package com.algodiary.controller;

import com.algodiary.service.InsightService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/insights")
public class InsightController {

    private final InsightService insightService;

    public InsightController(InsightService insightService) {
        this.insightService = insightService;
    }

    @GetMapping("/summary")
    public Map<String, String> summary() {
        return Map.of("content", insightService.summary());
    }

    @PostMapping("/refresh")
    public Map<String, String> refresh() {
        return Map.of("content", insightService.refreshSummary());
    }
}
