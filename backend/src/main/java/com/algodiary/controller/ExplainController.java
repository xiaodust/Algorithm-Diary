package com.algodiary.controller;

import com.algodiary.model.Problem;
import com.algodiary.service.ExplainService;
import com.algodiary.store.AlgoStore;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/explain")
public class ExplainController {

    private final ExplainService explainService;
    private final AlgoStore store;

    public ExplainController(ExplainService explainService, AlgoStore store) {
        this.explainService = explainService;
        this.store = store;
    }

    @PostMapping
    public ExplainResponse explain(@RequestBody ExplainRequest request) {
        Problem problem = store.findProblem(request.problemSlug()).orElse(null);
        String content = explainService.explain(problem, request.hintLevel());
        return new ExplainResponse(content);
    }

    public record ExplainRequest(String problemSlug, int hintLevel) {
    }

    public record ExplainResponse(String content) {
    }
}
