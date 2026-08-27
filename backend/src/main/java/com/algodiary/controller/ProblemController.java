package com.algodiary.controller;

import com.algodiary.service.ProblemTitleService;
import com.algodiary.store.AlgoStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemTitleService titleService;
    private final AlgoStore store;

    public ProblemController(ProblemTitleService titleService, AlgoStore store) {
        this.titleService = titleService;
        this.store = store;
    }

    @GetMapping("/titles")
    public Map<String, String> titles() {
        return titleService.getAllTitles();
    }

    @GetMapping("/solved")
    public List<SolvedProblem> solved() {
        Map<String, String> titles = titleService.getAllTitles();
        return store.findAllStates().stream()
                .filter(state -> state.acCount() > 0)
                .map(state -> new SolvedProblem(
                        state.problemSlug(),
                        titles.getOrDefault(state.problemSlug(), state.problemSlug())
                ))
                .sorted((a, b) -> a.title().compareToIgnoreCase(b.title()))
                .toList();
    }

    public record SolvedProblem(String slug, String title) {
    }
}
