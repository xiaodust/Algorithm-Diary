package com.algodiary.model;

public record MistakeNote(
        String problemSlug,
        String errorType,
        String stuckPoint,
        String lesson,
        String similarProblems
) {
}
