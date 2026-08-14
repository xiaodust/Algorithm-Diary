package com.algodiary.service;

import com.algodiary.support.InMemoryAlgoStore;
import org.junit.jupiter.api.Test;
import com.algodiary.dto.TopicStats;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsightServiceTest {

    @Test
    void ruleSummaryMentionsWeakTopics() {
        InsightService service = new InsightService(new InMemoryAlgoStore(), new AnalyzerService(), null, null);

        String summary = service.ruleBasedSummary(
                List.of(new TopicStats("dp", 5, 0.4, 3.0, 0.5, 0.0, true, false)),
                12,
                3
        );

        assertThat(summary).contains("薄弱题型", "dp", "AC 40%");
    }
}
