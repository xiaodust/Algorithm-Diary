package com.algodiary.model;

import java.util.List;

public record Problem(
        String slug,
        String title,
        Difficulty difficulty,
        List<String> tags,
        List<String> topics
) {
    public static Problem withDefaults(String slug, String title, Difficulty difficulty) {
        return new Problem(slug, title, difficulty, List.of(), List.of());
    }
}
