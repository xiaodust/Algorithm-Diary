package com.algodiary.controller;

import com.algodiary.dto.LeetCodeSettings;
import com.algodiary.service.SettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
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
    public LeetCodeSettings save(@Valid @RequestBody SaveRequest request) {
        return settingsService.saveLeetCodeSettings(
                request.session(),
                request.csrfToken(),
                request.cfClearance()
        );
    }

    public record SaveRequest(
            @Size(max = 8192, message = "session 长度不能超过 8192")
            String session,
            @Size(max = 8192, message = "csrfToken 长度不能超过 8192")
            String csrfToken,
            @Size(max = 8192, message = "cfClearance 长度不能超过 8192")
            String cfClearance
    ) {
    }
}
