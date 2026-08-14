package com.algodiary.controller;

import com.algodiary.service.ProblemTitleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/problems")
public class ProblemController {

    private final ProblemTitleService titleService;

    public ProblemController(ProblemTitleService titleService) {
        this.titleService = titleService;
    }

    @GetMapping("/titles")
    public Map<String, String> titles() {
        return titleService.getAllTitles();
    }
}
