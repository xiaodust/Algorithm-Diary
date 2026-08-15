package com.algodiary.controller;

import com.algodiary.model.*;
import com.algodiary.service.AnalyzerService;
import com.algodiary.service.PlannerService;
import com.algodiary.service.ProblemListService;
import com.algodiary.dto.TopicStats;
import com.algodiary.dto.CheckinStatus;
import com.algodiary.service.DailyPlanService;
import com.algodiary.service.GoalService;
import com.algodiary.store.AlgoStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/plan")
public class PlanController {

    private final PlannerService planner;
    private final ProblemListService listService;
    private final AnalyzerService analyzer;
    private final AlgoStore store;
    private final DailyPlanService dailyPlanService;
    private final GoalService goalService;

    public PlanController(
            PlannerService planner,
            ProblemListService listService,
            AnalyzerService analyzer,
            AlgoStore store,
            DailyPlanService dailyPlanService,
            GoalService goalService
    ) {
        this.planner = planner;
        this.listService = listService;
        this.analyzer = analyzer;
        this.store = store;
        this.dailyPlanService = dailyPlanService;
        this.goalService = goalService;
    }

    @GetMapping("/today")
    public DailyPlan today() {
        ProblemList active = listService.getActiveList();
        List<Problem> problems = store.findAllProblems();
        List<ProblemState> states = store.findAllStates();
        List<String> mistakeSlugs = store.findAllMistakes().stream()
                .map(MistakeNote::problemSlug)
                .toList();
        Set<String> weakTopics = weakTopicIds(problems, states);
        DailyPlan plan = planner.plan(
                active,
                problems,
                states,
                mistakeSlugs,
                weakTopics,
                Instant.now(),
                goalService.getDailyTarget()
        );
        store.savePlan(plan);
        return plan;
    }

    @GetMapping("/status")
    public CheckinStatus status() {
        return dailyPlanService.status();
    }

    @PostMapping("/complete")
    public CheckinStatus complete() {
        return dailyPlanService.completeToday();
    }

    private Set<String> weakTopicIds(List<Problem> problems, List<ProblemState> states) {
        Set<String> topicIds = store.findAllTopics().stream().map(Topic::id).collect(Collectors.toSet());
        return analyzer.analyze(problems, states, List.of(), topicIds, 1).stream()
                .filter(TopicStats::weak)
                .map(TopicStats::topicId)
                .collect(Collectors.toSet());
    }
}
