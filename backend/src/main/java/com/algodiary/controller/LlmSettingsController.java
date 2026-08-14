package com.algodiary.controller;

import com.algodiary.service.LlmSettingsService;
import com.algodiary.dto.LlmSettingsView;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/llm")
public class LlmSettingsController {

    private final LlmSettingsService settingsService;

    public LlmSettingsController(LlmSettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public LlmSettingsView get() {
        return settingsService.getSettings();
    }

    @PostMapping
    public LlmSettingsView save(@RequestBody SaveRequest request) {
        return settingsService.saveSettings(request.apiKey(), request.baseUrl(), request.model());
    }

    public record SaveRequest(String apiKey, String baseUrl, String model) {
    }
}
