package com.algodiary.model;

public record UserGoal(
        String activeListId,
        String targetType,
        int target
) {
}
