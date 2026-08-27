package com.algodiary.controller;

import com.algodiary.service.LlmSettingsService;
import com.algodiary.dto.LlmSettingsView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
    public LlmSettingsView save(@Valid @RequestBody SaveRequest request) {
        return settingsService.saveSettings(request.apiKey(), request.baseUrl(), request.model());
    }

    public record SaveRequest(
            @Size(max = 4096, message = "API Key 长度不能超过 4096")
            String apiKey,
            @Size(max = 2048, message = "Base URL 长度不能超过 2048")
            String baseUrl,
            @Size(max = 512, message = "模型名长度不能超过 512")
            String model
    ) {
    }
}
