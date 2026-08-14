package com.algodiary.leetcode;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeetCodeClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parsesUserProgressQuestions() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {"userProgressQuestionList":{"questions":[{
                    "frontendId":"1",
                    "title":"Two Sum",
                    "titleSlug":"two-sum",
                    "questionStatus":"SOLVED",
                    "lastResult":"AC",
                    "lastSubmittedAt":"2026-08-13T12:00:00Z"
                }]}}
                """);

        List<UserProgressQuestion> questions = LeetCodeClient.parseUserProgress(data);

        assertThat(questions).singleElement().satisfies(q -> {
            assertThat(q.titleSlug()).isEqualTo("two-sum");
            assertThat(q.lastResult()).isEqualTo("AC");
            assertThat(q.lastSubmittedAt()).isEqualTo("2026-08-13T12:00:00Z");
        });
    }

    @Test
    void parsesSubmissionPage() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {"submissionList":{
                    "lastKey":"key-1",
                    "hasNext":true,
                    "submissions":[{
                        "id":"123",
                        "title":"Two Sum",
                        "status":"WA",
                        "lang":"java",
                        "frontendId":"1",
                        "url":"/problems/two-sum/",
                        "timestamp":"2026-08-13T12:00:00Z"
                    }]
                }}
                """);

        SubmissionPage page = LeetCodeClient.parseSubmissionPage(data);

        assertThat(page.hasNext()).isTrue();
        assertThat(page.lastKey()).isEqualTo("key-1");
        assertThat(page.items()).singleElement().satisfies(s -> {
            assertThat(s.status()).isEqualTo("WA");
            assertThat(s.url()).isEqualTo("/problems/two-sum/");
            assertThat(s.timestamp()).isEqualTo("2026-08-13T12:00:00Z");
        });
    }

    @Test
    void parsesStudyPlan() throws Exception {
        JsonNode data = objectMapper.readTree("""
                {"studyPlanV2Detail":{
                    "name":"LeetCode 热题 100",
                    "planSubGroups":[{
                        "questions":[{
                            "titleSlug":"two-sum",
                            "translatedTitle":"两数之和",
                            "title":"Two Sum"
                        }]
                    }]
                }}
                """);

        StudyPlanSummary plan = LeetCodeClient.parseStudyPlan(data);

        assertThat(plan.name()).isEqualTo("LeetCode 热题 100");
        assertThat(plan.questions()).singleElement().satisfies(q -> {
            assertThat(q.titleSlug()).isEqualTo("two-sum");
            assertThat(q.translatedTitle()).isEqualTo("两数之和");
        });
    }
}
