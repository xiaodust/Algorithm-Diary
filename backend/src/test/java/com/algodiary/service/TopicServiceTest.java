package com.algodiary.service;

import com.algodiary.support.InMemoryAlgoStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TopicServiceTest {

    private final TopicService service = new TopicService(new InMemoryAlgoStore());

    @Test
    void classifiesSlidingWindowFromTitle() {
        assertThat(service.classify("minimum-window-substring", List.of()))
                .contains("sliding-window");
    }

    @Test
    void classifiesDynamicProgrammingFromTitle() {
        assertThat(service.classify("coin-change", List.of()))
                .contains("dynamic-programming");
    }

    @Test
    void mergesOfficialTags() {
        assertThat(service.classify("two-sum", List.of("hash-table")))
                .contains("hash-table", "two-pointers");
    }
}
