package com.algodiary.model;

import java.time.LocalDate;
import java.util.List;

public record DailyPlan(
        LocalDate date,
        List<PlanTask> coreTasks,
        List<PlanTask> bonusTasks,
        boolean completed
) {
}
