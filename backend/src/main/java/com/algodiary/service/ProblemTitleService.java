package com.algodiary.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ProblemTitleService {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourceLoader;

    public ProblemTitleService(JdbcTemplate jdbc, ObjectMapper objectMapper, ResourcePatternResolver resourceLoader) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void loadTitles() throws IOException {
        Resource[] resources = resourceLoader.getResources("classpath:titles/problem-titles.json");
        if (resources.length == 0) {
            return;
        }
        Map<String, String> titles = objectMapper.readValue(
                resources[0].getInputStream(),
                new TypeReference<Map<String, String>>() {}
        );
        titles.forEach(this::saveTitle);
    }

    public Map<String, String> getAllTitles() {
        Map<String, String> result = new LinkedHashMap<>();
        jdbc.query("SELECT slug, title_cn FROM problem_titles ORDER BY slug", rs -> {
            result.put(rs.getString("slug"), rs.getString("title_cn"));
        });
        return result;
    }

    public void saveTitle(String slug, String titleCn) {
        if (slug == null || slug.isBlank() || titleCn == null || titleCn.isBlank()) {
            return;
        }
        jdbc.update(
                "INSERT INTO problem_titles(slug, title_cn) VALUES (?, ?) "
                        + "ON CONFLICT(slug) DO UPDATE SET title_cn = excluded.title_cn",
                slug,
                titleCn
        );
    }
}
