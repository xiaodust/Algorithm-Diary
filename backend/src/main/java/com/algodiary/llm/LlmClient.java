package com.algodiary.llm;

import com.algodiary.config.LlmSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class LlmClient implements LlmGateway {

    private final RestClient restClient;
    private final LlmSettings settings;
    private final ObjectMapper objectMapper;

    public LlmClient(RestClient.Builder builder, LlmSettings settings, ObjectMapper objectMapper) {
        this.settings = settings;
        this.objectMapper = objectMapper;
        this.restClient = builder.build();
    }

    @Override
    public boolean isConfigured() {
        return settings.isConfigured();
    }

    @Override
    public String complete(String system, String user) {
        return chat(system, List.of(ChatMessage.user(user)));
    }

    @Override
    public String chat(String system, List<ChatMessage> history) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM API key 未配置");
        }
        StringBuilder full = new StringBuilder();
        chatStream(system, history, full::append);
        return full.toString();
    }

    @Override
    public void chatStream(String system, List<ChatMessage> history, Consumer<String> onDelta) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM API key 未配置");
        }
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", system));
        for (ChatMessage message : history) {
            messages.add(Map.of("role", message.role(), "content", message.content()));
        }

        Map<String, Object> body = Map.of(
                "model", settings.getModel(),
                "messages", messages,
                "temperature", 0.6,
                "stream", true
        );

        // 用 RestClient 的 exchange 在回调内同步读取事件流，
        // 避免返回的 InputStream 在回调外被 Spring 关闭
        restClient.post()
                .uri(normalizeBaseUrl(settings.getBaseUrl()) + "/chat/completions")
                .header("Authorization", "Bearer " + settings.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        throw new IllegalStateException("LLM 请求失败: HTTP " + response.getStatusCode().value());
                    }
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (!line.startsWith("data:")) {
                                continue;
                            }
                            String payload = line.substring(5).trim();
                            if (payload.isEmpty() || "[DONE]".equals(payload)) {
                                break;
                            }
                            try {
                                JsonNode node = objectMapper.readTree(payload);
                                JsonNode content = node.path("choices").path(0).path("delta").path("content");
                                if (content != null && !content.isMissingNode() && content.isTextual()) {
                                    onDelta.accept(content.asText());
                                }
                            } catch (Exception ignored) {
                                // 忽略单条 chunk 解析失败，继续读取
                            }
                        }
                    }
                    return null;
                });
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
