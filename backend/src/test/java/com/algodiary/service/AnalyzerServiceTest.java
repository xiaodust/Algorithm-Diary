package com.algodiary.service;

import com.algodiary.model.Difficulty;
import com.algodiary.model.Problem;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Review;
import org.junit.jupiter.api.Test;
import com.algodiary.dto.TopicStats;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnalyzerServiceTest {

    private final AnalyzerService service = new AnalyzerService();

    @Test
    void marksTopicWeakWhenAcRateIsLowAndAttemptsAreHigh() {
        List<Problem> problems = List.of(
                new Problem("a", "A", Difficulty.MEDIUM, List.of(), List.of("dp")),
                new Problem("b", "B", Difficulty.MEDIUM, List.of(), List.of("dp")),
                new Problem("c", "C", Difficulty.MEDIUM, List.of(), List.of("two-pointers"))
        );
        List<ProblemState> states = List.of(
                new ProblemState("a", 0, 0, 3, true, "wrong_answer", null, null, 0, null),
                new ProblemState("b", 1, 1, 2, false, null, null, null, 0, null),
                new ProblemState("c", 3, 3, 3, false, null, null, null, 0, null)
        );

        List<TopicStats> stats = service.analyze(problems, states, List.of(), Set.of("dp", "two-pointers"), 1);

        TopicStats dp = stats.stream().filter(s -> s.topicId().equals("dp")).findFirst().orElseThrow();
        TopicStats twoPointers = stats.stream().filter(s -> s.topicId().equals("two-pointers")).findFirst().orElseThrow();

        assertThat(dp.weak()).isTrue();
        assertThat(twoPointers.strong()).isTrue();
    }

    @Test
    void respectsMinimumSampleSize() {
        List<Problem> problems = List.of(
                new Problem("a", "A", Difficulty.MEDIUM, List.of(), List.of("dp"))
        );
        List<ProblemState> states = List.of(
                new ProblemState("a", 0, 0, 5, true, "wrong_answer", null, null, 0, null)
        );

        List<TopicStats> stats = service.analyze(problems, states, List.of(), Set.of("dp"), 3);

        assertThat(stats).isEmpty();
    }

    @Test
    void countsFailedReviewsAsForgetting() {
        Problem problem = new Problem("a", "A", Difficulty.MEDIUM, List.of(), List.of("dp"));
        ProblemState state = new ProblemState("a", 2, 1, 1, false, null, null, null, 1, Instant.now());
        Review failed = new Review("a", Instant.now(), false, "forgot");

        List<TopicStats> stats = service.analyze(List.of(problem), List.of(state), List.of(failed), Set.of("dp"), 1);

        assertThat(stats).singleElement().satisfies(s -> assertThat(s.forgetRate()).isEqualTo(1.0));
    }
}
