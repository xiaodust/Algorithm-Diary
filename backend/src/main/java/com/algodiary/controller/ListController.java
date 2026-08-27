package com.algodiary.controller;

import com.algodiary.leetcode.LeetCodeClient;
import com.algodiary.leetcode.SearchedProblem;
import com.algodiary.leetcode.StudyPlanInfo;
import com.algodiary.model.ProblemList;
import com.algodiary.dto.ListProgress;
import com.algodiary.service.ListRefreshService;
import com.algodiary.service.ProblemListService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lists")
public class ListController {

    private final ProblemListService service;
    private final ListRefreshService refreshService;
    private final LeetCodeClient leetCodeClient;

    public ListController(ProblemListService service, ListRefreshService refreshService, LeetCodeClient leetCodeClient) {
        this.service = service;
        this.refreshService = refreshService;
        this.leetCodeClient = leetCodeClient;
    }

    @GetMapping
    public List<ProblemList> getAll() {
        return service.getAllLists();
    }

    @GetMapping("/active")
    public ListProgress getActive() {
        ProblemList active = service.getActiveList();
        return service.getProgress(active);
    }

    @PostMapping("/refresh")
    public List<ProblemList> refresh() {
        return refreshService.refresh();
    }

    @PostMapping("/active")
    public ListProgress setActive(@RequestBody SetActiveRequest request) {
        service.setActiveList(request.listId());
        return service.getProgress(service.getActiveList());
    }

    @PostMapping
    public ProblemList create(@RequestBody SaveListRequest request) {
        return service.createCustomList(request.name(), request.slugs());
    }

    @PutMapping("/{listId}")
    public ProblemList update(@PathVariable String listId, @RequestBody SaveListRequest request) {
        return service.updateCustomList(listId, request.name(), request.slugs());
    }

    @DeleteMapping("/{listId}")
    public void delete(@PathVariable String listId) {
        service.deleteCustomList(listId);
    }

    @GetMapping("/search")
    public List<SearchedProblem> search(@RequestParam String keyword,
                                        @RequestParam(defaultValue = "20") int limit) {
        return leetCodeClient.searchProblems(keyword, limit);
    }

    @GetMapping("/study-plans")
    public List<StudyPlanInfo> studyPlans() {
        return leetCodeClient.fetchStudyPlans();
    }

    @PostMapping("/import")
    public ProblemList importPlan(@RequestBody ImportPlanRequest request) {
        return service.importStudyPlan(request.planSlug());
    }

    public record SetActiveRequest(String listId) {
    }

    public record SaveListRequest(String name, List<String> slugs) {
    }

    public record ImportPlanRequest(String planSlug) {
    }
}
