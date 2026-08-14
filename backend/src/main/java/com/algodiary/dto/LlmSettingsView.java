package com.algodiary.dto;

public record LlmSettingsView(
        boolean configured,
        String baseUrl,
        String model
) {
}
