package com.algodiary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "algodiary.llm")
public record LlmProperties(
        String apiKey,
        String baseUrl,
        String model
) {
}
