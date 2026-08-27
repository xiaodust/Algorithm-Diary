package com.algodiary.config;

import org.springframework.stereotype.Component;

@Component
public class LlmSettings {

    private volatile String apiKey;
    private volatile String baseUrl;
    private volatile String model;

    public LlmSettings(LlmProperties properties) {
        this.apiKey = properties.apiKey();
        this.baseUrl = properties.baseUrl();
        this.model = properties.model();
    }

    public synchronized void update(String apiKey, String baseUrl, String model) {
        this.apiKey = trimToNull(apiKey);
        this.baseUrl = trimToNull(baseUrl);
        this.model = trimToNull(model);
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl == null || baseUrl.isBlank() ? "https://api.openai.com/v1" : baseUrl;
    }

    public String getModel() {
        return model == null || model.isBlank() ? "gpt-4o-mini" : model;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
