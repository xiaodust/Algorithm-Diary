package com.algodiary.service;

import com.algodiary.model.ProblemState;
import com.algodiary.model.Review;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MistakeServiceTest {

    private final MistakeService service = new MistakeService();

    @Test
    void classifiesWrongAnswer() {
        assertThat(service.classifyError("WA")).isEqualTo("wrong_answer");
    }

    @Test
    void classifiesTimeout() {
        assertThat(service.classifyError("TLE")).isEqualTo("timeout");
    }

    @Test
    void nextReviewAdvancesOnPass() {
        Instant now = Instant.parse("2026-08-14T00:00:00Z");
        ProblemState state = ProblemState.empty("two-sum");

        Instant next = service.nextReview(state, true, now);

        assertThat(next).isEqualTo(now.plusSeconds(60 * 60 * 24));
    }

    @Test
    void nextReviewResetsOnFailure() {
        Instant now = Instant.parse("2026-08-14T00:00:00Z");
        ProblemState state = ProblemState.empty("two-sum");

        Instant next = service.nextReview(state, false, now);

        assertThat(next).isEqualTo(now.plusSeconds(60 * 60 * 24));
    }

    @Test
    void graduatesAfterTwoConsecutivePasses() {
        Instant now = Instant.parse("2026-08-14T00:00:00Z");

        boolean graduate = service.shouldGraduate(List.of(
                new Review("two-sum", now.minusSeconds(60 * 60), true, ""),
                new Review("two-sum", now, true, "")
        ));

        assertThat(graduate).isTrue();
    }

    @Test
    void doesNotGraduateWhenLatestReviewFailed() {
        Instant now = Instant.parse("2026-08-14T00:00:00Z");

        boolean graduate = service.shouldGraduate(List.of(
                new Review("two-sum", now.minusSeconds(60 * 60), true, ""),
                new Review("two-sum", now, false, "")
        ));

        assertThat(graduate).isFalse();
    }
}
