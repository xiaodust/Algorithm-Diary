package com.algodiary.dto;

public record Recommendation(
        String problemSlug,
        String reason,
        String url
) {
}
