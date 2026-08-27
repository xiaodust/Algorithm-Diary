package com.algodiary.service;

import com.algodiary.dto.TopicTrendPoint;
import com.algodiary.model.Difficulty;
import com.algodiary.model.Problem;
import com.algodiary.model.Review;
import com.algodiary.model.Submission;
import com.algodiary.support.InMemoryAlgoStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TopicTrendServiceTest {

    @Test
    void buildsCumulativeAcAndForgetRates() {
        InMemoryAlgoStore store = new InMemoryAlgoStore();
        store.saveProblem(new Problem("a", "A", Difficulty.MEDIUM, List.of(), List.of("dp")));
        store.saveProblem(new Problem("b", "B", Difficulty.MEDIUM, List.of(), List.of("dp")));

        Instant yesterday = Instant.now().minus(Duration.ofDays(1));
        store.saveSubmission(new Submission("a", "AC", "java", yesterday));
        store.saveSubmission(new Submission("a", "WA", "java", yesterday));
        store.saveSubmission(new Submission("b", "WA", "java", yesterday));
        store.saveReview(new Review("a", yesterday, false, "forgot"));

        TopicTrendService service = new TopicTrendService(store);
        List<TopicTrendPoint> trend = service.trend("dp", 7);

        assertThat(trend).hasSize(7);
        TopicTrendPoint latest = trend.getLast();
        assertThat(latest.solved()).isEqualTo(1);
        assertThat(latest.attempts()).isEqualTo(3);
        assertThat(latest.acRate()).isEqualTo(0.5);
        assertThat(latest.forgetRate()).isEqualTo(1.0);
    }
}
