package com.algodiary.controller;

import com.algodiary.model.Problem;
import com.algodiary.model.ProblemList;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Topic;
import com.algodiary.service.AnalyzerService;
import com.algodiary.service.ProblemListService;
import com.algodiary.dto.Recommendation;
import com.algodiary.service.AgentRecommendationService;
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

    private final AgentRecommendationService agentRecommendationService;

    public RecommendationController(AgentRecommendationService agentRecommendationService) {
        this.agentRecommendationService = agentRecommendationService;
    }

    @GetMapping
    public List<Recommendation> recommend(@RequestParam(defaultValue = "5") int limit) {
        return agentRecommendationService.recommend(limit);
    }
}
