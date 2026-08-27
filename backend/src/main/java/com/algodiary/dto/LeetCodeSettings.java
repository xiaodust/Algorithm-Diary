package com.algodiary.dto;

public record LeetCodeSettings(
        boolean configured,
        boolean hasCsrf,
        boolean hasCfClearance
) {
}
