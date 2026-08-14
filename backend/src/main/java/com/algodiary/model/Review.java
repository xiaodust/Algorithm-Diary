package com.algodiary.model;

import java.time.Instant;

public record Review(
        String problemSlug,
        Instant reviewedAt,
        boolean passed,
        String notes
) {
}
