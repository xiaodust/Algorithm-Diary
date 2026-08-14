package com.algodiary.llm;

import com.algodiary.config.LlmSettings;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class LlmClient implements LlmGateway {

    private final RestClient restClient;
    private final LlmSettings settings;

    public LlmClient(RestClient.Builder builder, LlmSettings settings) {
        this.settings = settings;
        this.restClient = builder.build();
    }

    @Override
    public boolean isConfigured() {
        return settings.isConfigured();
    }

    @Override
    public String complete(String system, String user) {
        if (!isConfigured()) {
            throw new IllegalStateException("LLM API key 未配置");
        }
        Map<String, Object> body = Map.of(
                "model", settings.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", system),
                        Map.of("role", "user", "content", user)
                ),
                "temperature", 0.4
        );
        JsonNode response = restClient.post()
                .uri(normalizeBaseUrl(settings.getBaseUrl()) + "/chat/completions")
                .header("Authorization", "Bearer " + settings.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);
        return response.path("choices").path(0).path("message").path("content").asText("");
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
