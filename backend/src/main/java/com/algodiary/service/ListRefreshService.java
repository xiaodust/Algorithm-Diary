package com.algodiary.service;

import com.algodiary.leetcode.LeetCodeClient;
import com.algodiary.leetcode.StudyPlanSummary;
import com.algodiary.model.ProblemList;
import com.algodiary.store.AlgoStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ListRefreshService {

    private static final List<ListSpec> BUILTIN_SPECS = List.of(
            new ListSpec("hot-100", "top-100-liked"),
            new ListSpec("interview-150", "top-interview-150"),
            new ListSpec("jianzhi-offer", "coding-interviews")
    );

    private final LeetCodeClient client;
    private final AlgoStore store;
    private final ProblemTitleService titleService;

    public ListRefreshService(LeetCodeClient client, AlgoStore store, ProblemTitleService titleService) {
        this.client = client;
        this.store = store;
        this.titleService = titleService;
    }

    public List<ProblemList> refresh() {
        for (ListSpec spec : BUILTIN_SPECS) {
            StudyPlanSummary summary = client.fetchStudyPlan(spec.planSlug());
            List<String> slugs = summary.questions().stream()
                    .map(com.algodiary.leetcode.StudyPlanQuestion::titleSlug)
                    .filter(slug -> slug != null && !slug.isBlank())
                    .toList();
            String name = summary.name() == null || summary.name().isBlank() ? spec.id() : summary.name();
            store.saveList(new ProblemList(spec.id(), name, "BUILTIN", slugs));
            summary.questions().forEach(question ->
                    titleService.saveTitle(question.titleSlug(), question.translatedTitle()));
        }
        return store.findAllLists();
    }

    private record ListSpec(String id, String planSlug) {
    }
}
