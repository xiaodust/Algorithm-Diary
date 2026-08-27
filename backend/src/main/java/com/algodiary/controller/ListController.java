package com.algodiary.controller;

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

    public ListController(ProblemListService service, ListRefreshService refreshService) {
        this.service = service;
        this.refreshService = refreshService;
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

    public record SetActiveRequest(String listId) {
    }
}
