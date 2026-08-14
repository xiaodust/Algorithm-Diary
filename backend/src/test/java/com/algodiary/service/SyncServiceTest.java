package com.algodiary.service;

import com.algodiary.model.ProblemState;
import com.algodiary.model.Submission;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SyncServiceTest {

    private final SyncService service = new SyncService(null, null, new MistakeService(), null);
    private final Instant now = Instant.parse("2026-08-14T00:00:00Z");

    @Test
    void buildsStateFromMixedSubmissions() {
        List<Submission> submissions = List.of(
                new Submission("two-sum", "WA", "java", Instant.parse("2026-08-13T10:00:00Z")),
                new Submission("two-sum", "AC", "java", Instant.parse("2026-08-13T11:00:00Z"))
        );

        ProblemState state = service.buildState("two-sum", submissions, now);

        assertThat(state.acCount()).isEqualTo(1);
        assertThat(state.attemptCount()).isEqualTo(2);
        assertThat(state.mistake()).isFalse();
        assertThat(state.firstAcAt()).isEqualTo("2026-08-13T11:00:00Z");
        assertThat(state.masteryLevel()).isEqualTo(1);
    }

    @Test
    void marksLatestWrongAnswerAsMistake() {
        List<Submission> submissions = List.of(
                new Submission("two-sum", "WA", "java", Instant.parse("2026-08-13T10:00:00Z")),
                new Submission("two-sum", "WA", "java", Instant.parse("2026-08-13T11:00:00Z"))
        );

        ProblemState state = service.buildState("two-sum", submissions, now);

        assertThat(state.mistake()).isTrue();
        assertThat(state.mistakeType()).isEqualTo("wrong_answer");
    }
}
