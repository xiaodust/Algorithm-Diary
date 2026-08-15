package com.algodiary.dto;

import java.util.List;

public record MemoryProfile(
        String generatedAt,
        String activeListId,
        int solvedCount,
        int mistakeCount,
        int dailyTarget,
        double pacePerDay,
        List<String> weakTopics,
        List<String> strongTopics,
        List<String> recentMistakeSlugs
) {
}
