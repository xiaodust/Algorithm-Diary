package com.algodiary.llm;

public interface LlmGateway {

    boolean isConfigured();

    String complete(String system, String user);
}
