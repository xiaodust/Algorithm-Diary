package com.algodiary.dto;

public record ListProgress(
        String listId,
        String listName,
        int total,
        int solved,
        int remaining,
        double pacePerDay,
        Integer estimatedDays
) {
    public double percent() {
        return total == 0 ? 0.0 : (double) solved / total * 100;
    }
}
