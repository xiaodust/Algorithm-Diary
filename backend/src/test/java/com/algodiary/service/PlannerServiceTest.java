package com.algodiary.service;

import com.algodiary.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlannerServiceTest {

    private final PlannerService service = new PlannerService();
    private final Instant now = Instant.parse("2026-08-14T00:00:00Z");

    @Test
    void prioritizesMistakeBeforeReviewBeforeWeakBeforeNew() {
        ProblemList list = new ProblemList("hot-100", "Hot 100", "BUILTIN", List.of("a", "b", "c", "d"));
        List<Problem> problems = List.of(
                new Problem("a", "A", Difficulty.EASY, List.of(), List.of("two-pointers")),
                new Problem("b", "B", Difficulty.MEDIUM, List.of(), List.of("binary-search")),
                new Problem("c", "C", Difficulty.MEDIUM, List.of(), List.of("dp")),
                new Problem("d", "D", Difficulty.HARD, List.of(), List.of("graph"))
        );
        List<ProblemState> states = List.of(
                new ProblemState("a", 1, 0, 2, true, "wrong_answer", null, null, 0, null),
                new ProblemState("b", 1, 1, 1, false, null, null, now.minusSeconds(60 * 60 * 24), 0, null),
                new ProblemState("c", 0, 0, 0, false, null, null, null, 0, null),
                new ProblemState("d", 0, 0, 0, false, null, null, null, 0, null)
        );
        Set<String> weakTopics = Set.of("dp");

        DailyPlan plan = service.plan(list, problems, states, List.of("a"), weakTopics, now);

        assertThat(plan.coreTasks())
                .extracting(PlanTask::reason)
                .containsExactly(TaskReason.MISTAKE, TaskReason.REVIEW);
        assertThat(plan.bonusTasks())
                .extracting(PlanTask::reason)
                .containsExactly(TaskReason.WEAK_TOPIC, TaskReason.LIST_NEW);
    }

    @Test
    void returnsEmptyTasksWhenNothingToPlan() {
        ProblemList list = new ProblemList("hot-100", "Hot 100", "BUILTIN", List.of());

        DailyPlan plan = service.plan(list, List.of(), List.of(), List.of(), Set.of(), now);

        assertThat(plan.coreTasks()).isEmpty();
        assertThat(plan.bonusTasks()).isEmpty();
    }
}
