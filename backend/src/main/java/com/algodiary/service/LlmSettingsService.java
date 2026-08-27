package com.algodiary.service;

import com.algodiary.config.LlmSettings;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.algodiary.dto.LlmSettingsView;

import java.util.List;

@Service
public class LlmSettingsService {

    private static final String KEY_API_KEY = "llm.api_key";
    private static final String KEY_BASE_URL = "llm.base_url";
    private static final String KEY_MODEL = "llm.model";

    private final JdbcTemplate jdbc;
    private final LlmSettings settings;

    public LlmSettingsService(JdbcTemplate jdbc, LlmSettings settings) {
        this.jdbc = jdbc;
        this.settings = settings;
    }

    @PostConstruct
    public void loadSavedSettings() {
        String apiKey = getValue(KEY_API_KEY);
        String baseUrl = getValue(KEY_BASE_URL);
        String model = getValue(KEY_MODEL);
        if (apiKey != null || baseUrl != null || model != null) {
            settings.update(apiKey, baseUrl, model);
        }
    }

    public LlmSettingsView getSettings() {
        return new LlmSettingsView(settings.isConfigured(), settings.getBaseUrl(), settings.getModel());
    }

    public LlmSettingsView saveSettings(String apiKey, String baseUrl, String model) {
        settings.update(apiKey, baseUrl, model);
        setValue(KEY_API_KEY, settings.getApiKey());
        setValue(KEY_BASE_URL, settings.getBaseUrl());
        setValue(KEY_MODEL, settings.getModel());
        return getSettings();
    }

    private void setValue(String key, String value) {
        if (value == null) {
            jdbc.update("DELETE FROM app_settings WHERE key = ?", key);
            return;
        }
        jdbc.update(
                "INSERT INTO app_settings(key, value) VALUES (?, ?) "
                        + "ON CONFLICT(key) DO UPDATE SET value = excluded.value",
                key,
                value
        );
    }

    private String getValue(String key) {
        List<String> values = jdbc.query(
                "SELECT value FROM app_settings WHERE key = ?",
                (rs, rowNum) -> rs.getString("value"),
                key
        );
        return values.isEmpty() ? null : values.getFirst();
    }
}
