package com.algodiary.dto;

import java.time.LocalDate;

public record CheckinStatus(
        LocalDate date,
        boolean completed,
        int streak
) {
}
