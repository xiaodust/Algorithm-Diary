package com.algodiary.service;

import com.algodiary.model.*;
import org.junit.jupiter.api.Test;
import com.algodiary.dto.Recommendation;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationServiceTest {

    private final RecommendationService service = new RecommendationService();

    @Test
    void recommendsWeakTopicProblemsInsideActiveListFirst() {
        ProblemList list = new ProblemList("hot-100", "Hot 100", "BUILTIN", List.of("in-list", "outside"));
        List<Problem> problems = List.of(
                new Problem("in-list", "In List", Difficulty.MEDIUM, List.of(), List.of("dp")),
                new Problem("outside", "Outside", Difficulty.MEDIUM, List.of(), List.of("dp")),
                new Problem("solved", "Solved", Difficulty.MEDIUM, List.of(), List.of("dp"))
        );
        List<ProblemState> states = List.of(
                ProblemState.empty("in-list"),
                ProblemState.empty("outside"),
                new ProblemState("solved", 2, 1, 1, false, null, null, null, 0, null)
        );

        List<Recommendation> recommendations = service.recommend(list, problems, states, Set.of("dp"), 3);

        assertThat(recommendations).extracting(Recommendation::problemSlug)
                .containsExactly("in-list", "outside");
        assertThat(recommendations.getFirst().url())
                .isEqualTo("https://leetcode.cn/problems/in-list/");
    }
}
