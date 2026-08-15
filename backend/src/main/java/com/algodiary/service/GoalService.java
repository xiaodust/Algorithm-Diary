package com.algodiary.service;

import com.algodiary.dto.GoalView;
import com.algodiary.dto.ListProgress;
import com.algodiary.model.ProblemList;
import com.algodiary.model.UserGoal;
import com.algodiary.store.AlgoStore;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class GoalService {

    public static final String TARGET_COMPLETE_LIST = "COMPLETE_LIST";
    public static final String TARGET_SOLVE_COUNT = "SOLVE_COUNT";

    private static final Set<String> ALLOWED_TARGET_TYPES = Set.of(
            TARGET_COMPLETE_LIST,
            TARGET_SOLVE_COUNT
    );

    private final AlgoStore store;
    private final ProblemListService listService;

    public GoalService(AlgoStore store, ProblemListService listService) {
        this.store = store;
        this.listService = listService;
    }

    public GoalView getGoalView() {
        ProblemList active = listService.getActiveList();
        UserGoal goal = store.findGoal()
                .orElseGet(() -> {
                    UserGoal created = new UserGoal(
                            active.id(),
                            TARGET_COMPLETE_LIST,
                            active.problemSlugs().size(),
                            3
                    );
                    store.saveGoal(created);
                    return created;
                });
        ListProgress progress = listService.getProgress(active);
        int target = goal.target() > 0 ? goal.target() : active.problemSlugs().size();
        int solved = Math.min(progress.solved(), target);
        int remaining = Math.max(0, target - progress.solved());
        double percent = target == 0 ? 0.0 : (double) progress.solved() / target * 100.0;
        Integer estimatedDays = progress.estimatedDays();
        if (estimatedDays == null && progress.pacePerDay() > 0) {
            estimatedDays = (int) Math.ceil(remaining / progress.pacePerDay());
        }

        return new GoalView(
                active.id(),
                active.name(),
                goal.targetType(),
                target,
                goal.dailyTarget(),
                active.problemSlugs().size(),
                solved,
                remaining,
                estimatedDays,
                Math.min(100.0, percent)
        );
    }

    public GoalView saveGoal(String targetType, int target, int dailyTarget) {
        String normalizedType = targetType == null ? TARGET_COMPLETE_LIST : targetType.trim().toUpperCase();
        if (!ALLOWED_TARGET_TYPES.contains(normalizedType)) {
            throw new IllegalArgumentException("不支持的目标类型: " + targetType);
        }
        if (target < 1 || target > 10000) {
            throw new IllegalArgumentException("长期目标题数必须在 1 到 10000 之间");
        }
        if (dailyTarget < 1 || dailyTarget > 50) {
            throw new IllegalArgumentException("每日目标题数必须在 1 到 50 之间");
        }

        ProblemList active = listService.getActiveList();
        store.saveGoal(new UserGoal(active.id(), normalizedType, target, dailyTarget));
        return getGoalView();
    }

    public int getDailyTarget() {
        return store.findGoal()
                .map(UserGoal::dailyTarget)
                .orElse(3);
    }
}
