package com.algodiary.leetcode;

import java.time.Instant;

public record SubmissionItem(
        String id,
        String title,
        String status,
        String lang,
        String frontendId,
        String url,
        Instant timestamp
) {
}
