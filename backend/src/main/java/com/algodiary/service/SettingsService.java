package com.algodiary.service;

import com.algodiary.config.LeetCodeCredentials;
import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import com.algodiary.dto.LeetCodeSettings;

import java.util.List;

@Service
public class SettingsService {

    private static final String KEY_SESSION = "leetcode.session";
    private static final String KEY_CSRF = "leetcode.csrf";
    private static final String KEY_CF_CLEARANCE = "leetcode.cf_clearance";

    private final JdbcTemplate jdbc;
    private final LeetCodeCredentials credentials;

    public SettingsService(JdbcTemplate jdbc, LeetCodeCredentials credentials) {
        this.jdbc = jdbc;
        this.credentials = credentials;
    }

    @PostConstruct
    public void loadSavedSettings() {
        String session = getValue(KEY_SESSION);
        String csrf = getValue(KEY_CSRF);
        String cfClearance = getValue(KEY_CF_CLEARANCE);
        if (session != null || csrf != null || cfClearance != null) {
            credentials.update(session, csrf, cfClearance);
        }
    }

    public LeetCodeSettings getLeetCodeSettings() {
        return new LeetCodeSettings(
                credentials.isConfigured(),
                credentials.getCsrfToken() != null,
                credentials.getCfClearance() != null
        );
    }

    public LeetCodeSettings saveLeetCodeSettings(String session, String csrfToken, String cfClearance) {
        credentials.update(session, csrfToken, cfClearance);
        setValue(KEY_SESSION, credentials.getSession());
        setValue(KEY_CSRF, credentials.getCsrfToken());
        setValue(KEY_CF_CLEARANCE, credentials.getCfClearance());
        return getLeetCodeSettings();
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
