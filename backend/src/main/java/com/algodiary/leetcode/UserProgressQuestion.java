package com.algodiary.leetcode;

import java.time.Instant;

public record UserProgressQuestion(
        String frontendId,
        String title,
        String translatedTitle,
        String titleSlug,
        String questionStatus,
        String lastResult,
        Instant lastSubmittedAt
) {
}
