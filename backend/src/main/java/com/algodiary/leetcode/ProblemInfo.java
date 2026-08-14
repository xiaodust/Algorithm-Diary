package com.algodiary.leetcode;

import java.util.List;

public record ProblemInfo(
        String titleSlug,
        String title,
        String difficulty,
        List<String> tags
) {
}
