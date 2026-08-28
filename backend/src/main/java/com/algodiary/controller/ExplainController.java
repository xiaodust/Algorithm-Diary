package com.algodiary.controller;

import com.algodiary.model.Problem;
import com.algodiary.service.ExplainService;
import com.algodiary.service.TutorService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/explain")
public class ExplainController {

    private final ExplainService explainService;
    private final TutorService tutorService;

    public ExplainController(ExplainService explainService, TutorService tutorService) {
        this.explainService = explainService;
        this.tutorService = tutorService;
    }

    @PostMapping
    public ExplainResponse explain(@RequestBody ExplainRequest request) {
        Problem problem = tutorService.resolveProblem(request.problemSlug());
        String content = explainService.explain(problem, request.hintLevel());
        return new ExplainResponse(content);
    }

    public record ExplainRequest(String problemSlug, int hintLevel) {
    }

    public record ExplainResponse(String content) {
    }
}
