package com.algodiary.model;

import java.time.Instant;

public record TutorSession(
        String id,
        String name,
        Instant createdAt,
        Instant updatedAt
) {
}
