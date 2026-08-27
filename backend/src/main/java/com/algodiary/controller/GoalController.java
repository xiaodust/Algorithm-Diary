package com.algodiary.controller;

import com.algodiary.dto.GoalView;
import com.algodiary.service.GoalService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goal")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public GoalView get() {
        return goalService.getGoalView();
    }

    @PostMapping
    public GoalView save(@Valid @RequestBody SaveGoalRequest request) {
        return goalService.saveGoal(request.targetType(), request.target(), request.dailyTarget());
    }

    public record SaveGoalRequest(
            @NotBlank(message = "目标类型不能为空")
            String targetType,
            @Min(value = 1, message = "长期目标题数不能小于 1")
            @Max(value = 10000, message = "长期目标题数不能大于 10000")
            int target,
            @Min(value = 1, message = "每日目标题数不能小于 1")
            @Max(value = 50, message = "每日目标题数不能大于 50")
            int dailyTarget
    ) {
    }
}
