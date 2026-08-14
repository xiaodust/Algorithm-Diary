package com.algodiary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "algodiary.leetcode")
public record LeetCodeProperties(
        String graphqlUrl,
        String session,
        String csrfToken,
        String cfClearance
) {
}
