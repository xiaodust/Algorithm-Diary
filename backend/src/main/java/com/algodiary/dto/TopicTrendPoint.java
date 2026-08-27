package com.algodiary.dto;

import java.time.LocalDate;

public record TopicTrendPoint(
        LocalDate date,
        int solved,
        int attempts,
        double acRate,
        double forgetRate
) {
}
