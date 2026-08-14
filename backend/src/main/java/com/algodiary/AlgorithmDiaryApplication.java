package com.algodiary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class AlgorithmDiaryApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlgorithmDiaryApplication.class, args);
    }
}
