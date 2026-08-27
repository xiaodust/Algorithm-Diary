package com.algodiary.dto;

public record GoalView(
        String activeListId,
        String listName,
        String targetType,
        int target,
        int dailyTarget,
        int total,
        int solved,
        int remaining,
        Integer estimatedDays,
        double percent
) {
}
