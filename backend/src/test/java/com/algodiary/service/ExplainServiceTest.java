package com.algodiary.service;

import com.algodiary.model.Difficulty;
import com.algodiary.model.Problem;
import org.junit.jupiter.api.Test;
import com.algodiary.llm.LlmGateway;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExplainServiceTest {

    private final Problem problem = new Problem("two-sum", "Two Sum", Difficulty.EASY, List.of(), List.of("hash-table"));

    @Test
    void fallsBackToRuleBasedWhenLlmNotConfigured() {
        ExplainService service = new ExplainService(new FakeLlmGateway(false, ""));

        String result = service.explain(problem, 0);

        assertThat(result).contains("Two Sum");
    }

    @Test
    void usesLlmWhenConfigured() {
        ExplainService service = new ExplainService(new FakeLlmGateway(true, "先尝试哈希表"));

        String result = service.explain(problem, 1);

        assertThat(result).isEqualTo("先尝试哈希表");
    }

    private record FakeLlmGateway(boolean configured, String answer) implements LlmGateway {
        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public String complete(String system, String user) {
            return answer;
        }

        @Override
        public String chat(String system, List<ChatMessage> history) {
            return answer;
        }

        @Override
        public void chatStream(String system, List<ChatMessage> history, java.util.function.Consumer<String> onDelta) {
            onDelta.accept(answer);
        }
    }
}
