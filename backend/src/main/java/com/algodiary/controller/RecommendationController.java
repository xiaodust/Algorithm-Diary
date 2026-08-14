package com.algodiary.controller;

import com.algodiary.model.Problem;
import com.algodiary.model.ProblemList;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Topic;
import com.algodiary.service.AnalyzerService;
import com.algodiary.service.ProblemListService;
import com.algodiary.dto.Recommendation;
import com.algodiary.service.RecommendationService;
import com.algodiary.dto.TopicStats;
import com.algodiary.store.AlgoStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final ProblemListService listService;
    private final AnalyzerService analyzer;
    private final AlgoStore store;

    public RecommendationController(RecommendationService recommendationService, ProblemListService listService, AnalyzerService analyzer, AlgoStore store) {
        this.recommendationService = recommendationService;
        this.listService = listService;
        this.analyzer = analyzer;
        this.store = store;
    }

    @GetMapping
    public List<Recommendation> recommend(@RequestParam(defaultValue = "5") int limit) {
        ProblemList active = listService.getActiveList();
        List<Problem> problems = store.findAllProblems();
        List<ProblemState> states = store.findAllStates();
        Set<String> topicIds = store.findAllTopics().stream().map(Topic::id).collect(Collectors.toSet());
        Set<String> weakTopics = analyzer.analyze(problems, states, List.of(), topicIds, 1).stream()
                .filter(TopicStats::weak)
                .map(TopicStats::topicId)
                .collect(Collectors.toSet());
        return recommendationService.recommend(active, problems, states, weakTopics, limit);
    }
}
