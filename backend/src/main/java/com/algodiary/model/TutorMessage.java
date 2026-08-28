package com.algodiary.model;

import java.time.Instant;

public record TutorMessage(
        long id,
        String sessionId,
        String role,
        String content,
        Instant createdAt
) {
}
