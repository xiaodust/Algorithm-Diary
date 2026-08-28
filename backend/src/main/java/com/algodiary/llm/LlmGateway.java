package com.algodiary.llm;

import java.util.List;
import java.util.function.Consumer;

public interface LlmGateway {

    boolean isConfigured();

    String complete(String system, String user);

    String chat(String system, List<ChatMessage> history);

    void chatStream(String system, List<ChatMessage> history, Consumer<String> onDelta);

    record ChatMessage(String role, String content) {
        public static ChatMessage user(String content) {
            return new ChatMessage("user", content);
        }

        public static ChatMessage assistant(String content) {
            return new ChatMessage("assistant", content);
        }
    }
}
