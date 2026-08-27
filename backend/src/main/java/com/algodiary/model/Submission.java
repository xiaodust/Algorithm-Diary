package com.algodiary.model;

import java.time.Instant;

public record Submission(
        String problemSlug,
        String status,
        String lang,
        Instant submittedAt
) {
    public boolean isAccepted() {
        return "AC".equalsIgnoreCase(status) || "Accepted".equalsIgnoreCase(status);
    }
}
