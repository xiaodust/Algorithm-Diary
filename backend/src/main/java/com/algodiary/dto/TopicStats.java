package com.algodiary.dto;

public record TopicStats(
        String topicId,
        int problemCount,
        double acRate,
        double avgAttempts,
        double masteryAvg,
        double forgetRate,
        boolean weak,
        boolean strong
) {
}
