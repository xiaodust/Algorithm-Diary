package com.algodiary;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AlgorithmDiaryApplication {

    public static void main(String[] args) {
        run(args);
    }

    public static ConfigurableApplicationContext run(String[] args) {
        String dataDir = resolveDataDir();
        try {
            Files.createDirectories(Path.of(dataDir));
        } catch (Exception ex) {
            throw new IllegalStateException("无法创建数据目录: " + dataDir, ex);
        }
        System.setProperty("APP_DATA_DIR", dataDir);

        return SpringApplication.run(AlgorithmDiaryApplication.class, args);
    }

    private static String resolveDataDir() {
        String configured = System.getProperty("APP_DATA_DIR");
        if (configured == null || configured.isBlank()) {
            configured = System.getenv("APP_DATA_DIR");
        }
        if (configured != null && !configured.isBlank()) {
            return configured.trim();
        }

        Path devData = Path.of("data");
        if (Files.isDirectory(devData)) {
            return devData.toAbsolutePath().toString();
        }

        Path repoDevData = Path.of("backend", "data");
        if (Files.isDirectory(repoDevData)) {
            return repoDevData.toAbsolutePath().toString();
        }

        String localAppData = System.getenv("LOCALAPPDATA");
        Path userData;
        if (localAppData != null && !localAppData.isBlank()) {
            userData = Path.of(localAppData, "AlgorithmDiary", "data");
        } else {
            userData = Path.of(System.getProperty("user.home"), ".algorithm-diary", "data");
        }
        return userData.toAbsolutePath().toString();
    }
}
