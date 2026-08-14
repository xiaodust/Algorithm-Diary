package com.algodiary.dto;

public record TopicProblem(
        String slug,
        String title,
        String difficulty,
        boolean solved
) {
}
