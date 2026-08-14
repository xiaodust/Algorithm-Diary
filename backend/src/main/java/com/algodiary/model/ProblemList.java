package com.algodiary.model;

import java.util.List;

public record ProblemList(
        String id,
        String name,
        String source,
        List<String> problemSlugs
) {
}
