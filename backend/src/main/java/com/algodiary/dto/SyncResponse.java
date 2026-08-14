package com.algodiary.dto;

public record SyncResponse(
        boolean demo,
        int problems,
        int submissions,
        String message
) {
}
