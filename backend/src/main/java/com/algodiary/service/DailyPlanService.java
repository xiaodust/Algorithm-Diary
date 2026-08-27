package com.algodiary.service;

import com.algodiary.model.DailyPlan;
import com.algodiary.model.PlanTask;
import com.algodiary.model.Submission;
import com.algodiary.store.AlgoStore;
import org.springframework.stereotype.Service;
import com.algodiary.dto.CheckinStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class DailyPlanService {

    private final AlgoStore store;

    public DailyPlanService(AlgoStore store) {
        this.store = store;
    }

    public CheckinStatus status() {
        return status(Instant.now());
    }

    public CheckinStatus completeToday() {
        return completeToday(Instant.now());
    }

    public CheckinStatus completeToday(Instant now) {
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
        DailyPlan existing = store.findPlan(today).orElse(null);
        List<PlanTask> core = existing == null ? List.of() : existing.coreTasks();
        List<PlanTask> bonus = existing == null ? List.of() : existing.bonusTasks();
        store.savePlan(new DailyPlan(today, core, bonus, true));
        return status(now);
    }

    public CheckinStatus autoCompleteIfDone(Instant now) {
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
        DailyPlan plan = store.findPlan(today).orElse(null);
        if (plan == null || plan.completed() || plan.coreTasks() == null || plan.coreTasks().isEmpty()) {
            return status(now);
        }

        Instant startOfToday = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        boolean allDone = plan.coreTasks().stream()
                .allMatch(task -> isSolvedToday(task.problemSlug(), startOfToday, now));

        if (allDone) {
            store.savePlan(new DailyPlan(today, plan.coreTasks(), plan.bonusTasks(), true));
        }
        return status(now);
    }

    private CheckinStatus status(Instant now) {
        LocalDate today = now.atZone(ZoneId.systemDefault()).toLocalDate();
        List<LocalDate> completedDates = store.findCompletedPlanDates();
        return new CheckinStatus(today, completedDates.contains(today), computeStreak(completedDates, today));
    }

    private boolean isSolvedToday(String slug, Instant startOfToday, Instant now) {
        return store.findSubmissions(slug).stream()
                .filter(Submission::isAccepted)
                .anyMatch(submission ->
                        submission.submittedAt() != null
                                && !submission.submittedAt().isBefore(startOfToday)
                                && !submission.submittedAt().isAfter(now)
                );
    }

    public static int computeStreak(List<LocalDate> completedDates, LocalDate today) {
        Set<LocalDate> dates = new HashSet<>(completedDates);
        int streak = 0;
        LocalDate day = dates.contains(today) ? today : today.minusDays(1);
        while (dates.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
