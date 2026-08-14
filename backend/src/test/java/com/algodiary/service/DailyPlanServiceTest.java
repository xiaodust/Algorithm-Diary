package com.algodiary.service;

import com.algodiary.model.DailyPlan;
import com.algodiary.model.PlanTask;
import com.algodiary.model.Submission;
import com.algodiary.model.TaskReason;
import com.algodiary.support.InMemoryAlgoStore;
import org.junit.jupiter.api.Test;
import com.algodiary.dto.CheckinStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DailyPlanServiceTest {

    @Test
    void streakCountsConsecutiveDaysIncludingToday() {
        LocalDate today = LocalDate.parse("2026-08-14");

        int streak = DailyPlanService.computeStreak(
                List.of(today, today.minusDays(1), today.minusDays(2)),
                today
        );

        assertThat(streak).isEqualTo(3);
    }

    @Test
    void streakStartsFromYesterdayWhenTodayNotCompleted() {
        LocalDate today = LocalDate.parse("2026-08-14");

        int streak = DailyPlanService.computeStreak(
                List.of(today.minusDays(1), today.minusDays(2)),
                today
        );

        assertThat(streak).isEqualTo(2);
    }

    @Test
    void autoCompletesPlanWhenCoreTasksSolvedToday() {
        InMemoryAlgoStore store = new InMemoryAlgoStore();
        DailyPlanService service = new DailyPlanService(store);
        Instant now = Instant.parse("2026-08-14T10:00:00Z");
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();

        store.savePlan(new DailyPlan(today, List.of(new PlanTask("two-sum", TaskReason.MISTAKE)), List.of(), false));
        store.saveSubmission(new Submission("two-sum", "AC", "java", now.minusSeconds(60)));

        CheckinStatus status = service.autoCompleteIfDone(now);

        assertThat(status.completed()).isTrue();
        assertThat(status.streak()).isEqualTo(1);
    }
}
