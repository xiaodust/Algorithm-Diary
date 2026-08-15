package com.algodiary;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest
class AlgorithmDiaryApplicationTest {

    private static final Path DATA_DIR = createTempDataDir();

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("APP_DATA_DIR", () -> DATA_DIR.toString());
    }

    @Test
    void contextLoads() {
    }

    private static Path createTempDataDir() {
        try {
            return Files.createTempDirectory("algo-diary-test-");
        } catch (Exception ex) {
            throw new IllegalStateException("无法创建测试数据目录", ex);
        }
    }
}
