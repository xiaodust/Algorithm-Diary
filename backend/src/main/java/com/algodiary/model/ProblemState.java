package com.algodiary.model;

import java.time.Instant;

public record ProblemState(
        String problemSlug,
        int masteryLevel,
        int acCount,
        int attemptCount,
        boolean mistake,
        String mistakeType,
        Instant lastReviewAt,
        Instant nextReviewAt,
        int reviewCount,
        Instant firstAcAt
) {
    public static ProblemState empty(String problemSlug) {
        return new ProblemState(problemSlug, 0, 0, 0, false, null, null, null, 0, null);
    }
}
