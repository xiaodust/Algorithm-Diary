package com.algodiary.controller;

import com.algodiary.dto.LeetCodeSettings;
import com.algodiary.service.SettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/leetcode")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public LeetCodeSettings get() {
        return settingsService.getLeetCodeSettings();
    }

    @PostMapping
    public LeetCodeSettings save(@RequestBody SaveRequest request) {
        return settingsService.saveLeetCodeSettings(
                request.session(),
                request.csrfToken(),
                request.cfClearance()
        );
    }

    public record SaveRequest(String session, String csrfToken, String cfClearance) {
    }
}
