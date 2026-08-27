package com.algodiary.config;

import org.springframework.stereotype.Component;

@Component
public class LeetCodeCredentials {

    private volatile String session;
    private volatile String csrfToken;
    private volatile String cfClearance;

    public LeetCodeCredentials(LeetCodeProperties properties) {
        this.session = properties.session();
        this.csrfToken = properties.csrfToken();
        this.cfClearance = properties.cfClearance();
    }

    public synchronized void update(String session, String csrfToken, String cfClearance) {
        this.session = trimToNull(session);
        this.csrfToken = trimToNull(csrfToken);
        this.cfClearance = trimToNull(cfClearance);
    }

    public boolean isConfigured() {
        return session != null && !session.isBlank();
    }

    public String getSession() {
        return session;
    }

    public String getCsrfToken() {
        return csrfToken;
    }

    public String getCfClearance() {
        return cfClearance;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
