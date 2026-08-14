package com.algodiary.controller;

import com.algodiary.model.Problem;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Topic;
import com.algodiary.dto.TopicProblem;
import com.algodiary.service.AnalyzerService;
import com.algodiary.service.TopicService;
import com.algodiary.dto.TopicStats;
import com.algodiary.store.AlgoStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/topics")
public class TopicController {

    private final TopicService topicService;
    private final AnalyzerService analyzer;
    private final AlgoStore store;

    public TopicController(TopicService topicService, AnalyzerService analyzer, AlgoStore store) {
        this.topicService = topicService;
        this.analyzer = analyzer;
        this.store = store;
    }

    @GetMapping
    public List<Topic> getAll() {
        return topicService.findAllTopics();
    }

    @GetMapping("/stats")
    public List<TopicStats> stats() {
        List<Problem> problems = store.findAllProblems();
        List<ProblemState> states = store.findAllStates();
        Set<String> topicIds = topicService.findAllTopics().stream().map(Topic::id).collect(Collectors.toSet());
        return analyzer.analyze(problems, states, List.of(), topicIds, 1);
    }

    @GetMapping("/{topicId}/problems")
    public List<TopicProblem> problems(@PathVariable String topicId) {
        return store.findAllProblems().stream()
                .filter(problem -> problem.topics() != null && problem.topics().contains(topicId))
                .map(problem -> new TopicProblem(
                        problem.slug(),
                        problem.title(),
                        problem.difficulty() == null ? null : problem.difficulty().name(),
                        store.findState(problem.slug())
                                .map(state -> state.acCount() > 0)
                                .orElse(false)
                ))
                .toList();
    }
}
