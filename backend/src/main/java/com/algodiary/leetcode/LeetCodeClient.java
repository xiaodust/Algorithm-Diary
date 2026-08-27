package com.algodiary.leetcode;

import com.algodiary.config.LeetCodeProperties;
import com.algodiary.config.LeetCodeCredentials;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class LeetCodeClient {

    private static final Logger log = LoggerFactory.getLogger(LeetCodeClient.class);
    private static final String BROWSER_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    private static final String ORIGIN = "https://leetcode.cn";
    private static final int MAX_ATTEMPTS = 3;
    private static final long MIN_REQUEST_INTERVAL_MS = 500L;

    private final RestClient restClient;
    private final LeetCodeProperties properties;
    private final LeetCodeCredentials credentials;
    private volatile long lastRequestAt;

    public LeetCodeClient(RestClient.Builder builder, LeetCodeProperties properties, LeetCodeCredentials credentials) {
        this.restClient = builder.baseUrl(properties.graphqlUrl()).build();
        this.properties = properties;
        this.credentials = credentials;
    }

    public List<UserProgressQuestion> fetchUserProgress() {
        Map<String, Object> variables = Map.of(
                "filters", Map.of("skip", 0, "limit", 4000)
        );
        JsonNode data = postGraphQL(
                "userProgressQuestionList",
                """
                query userProgressQuestionList($filters: UserProgressQuestionListInput) {
                  userProgressQuestionList(filters: $filters) {
                    questions {
                      frontendId
                      title
                      translatedTitle
                      titleSlug
                      questionStatus
                      lastResult
                      lastSubmittedAt
                    }
                  }
                }
                """,
                variables
        );
        return parseUserProgress(data.path("data"));
    }

    public SubmissionPage fetchSubmissionsPage(int offset, int limit, String lastKey) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("offset", offset);
        variables.put("limit", limit);
        variables.put("lastKey", lastKey);
        variables.put("questionSlug", null);

        JsonNode data = postGraphQL(
                "submissionList",
                """
                query submissionList($offset: Int!, $limit: Int!, $lastKey: String, $questionSlug: String) {
                  submissionList(offset: $offset, limit: $limit, lastKey: $lastKey, questionSlug: $questionSlug) {
                    lastKey
                    hasNext
                    submissions {
                      id
                      title
                      status
                      lang
                      frontendId
                      url
                      timestamp
                    }
                  }
                }
                """,
                variables
        );
        return parseSubmissionPage(data.path("data"));
    }

    public Optional<DailyChallenge> fetchDailyChallenge() {
        JsonNode data = postGraphQL(
                "activeDailyCodingChallengeQuestion",
                """
                query questionOfToday {
                  activeDailyCodingChallengeQuestion {
                    date
                    link
                    question {
                      titleSlug
                      title
                      difficulty
                    }
                  }
                }
                """,
                Map.of()
        );
        JsonNode node = data.path("data").path("activeDailyCodingChallengeQuestion");
        if (node.isMissingNode() || node.isNull()) {
            return Optional.empty();
        }
        JsonNode question = node.path("question");
        return Optional.of(new DailyChallenge(
                node.path("date").asText(null),
                node.path("link").asText(null),
                question.path("titleSlug").asText(null),
                question.path("title").asText(null),
                question.path("difficulty").asText(null)
        ));
    }

    public Optional<ProblemInfo> fetchProblem(String titleSlug) {
        JsonNode data = postGraphQL(
                "questionData",
                """
                query questionData($titleSlug: String!) {
                  question(titleSlug: $titleSlug) {
                    title
                    titleSlug
                    difficulty
                    topicTags { slug }
                  }
                }
                """,
                Map.of("titleSlug", titleSlug)
        );
        JsonNode question = data.path("data").path("question");
        if (question.isMissingNode() || question.isNull()) {
            return Optional.empty();
        }
        List<String> tags = new ArrayList<>();
        question.path("topicTags").forEach(tag -> tags.add(tag.path("slug").asText(null)));
        return Optional.of(new ProblemInfo(
                question.path("titleSlug").asText(titleSlug),
                question.path("title").asText(null),
                question.path("difficulty").asText(null),
                tags
        ));
    }

    public StudyPlanSummary fetchStudyPlan(String planSlug) {
        JsonNode data = postGraphQL(
                "studyPlanV2Detail",
                """
                query studyPlanV2Detail($planSlug: String!) {
                  studyPlanV2Detail(planSlug: $planSlug) {
                    name
                    slug
                    questionNum
                    planSubGroups {
                      questions {
                        titleSlug
                        translatedTitle
                        title
                      }
                    }
                  }
                }
                """,
                Map.of("planSlug", planSlug)
        );
        return parseStudyPlan(data.path("data"));
    }

    public List<StudyPlanInfo> fetchStudyPlans() {
        // leetcode.cn 未提供公开的官方题单列表 GraphQL 字段，这里内置常用的官方题单 slug
        // 详情可通过 fetchStudyPlan(planSlug) 拉取
        return List.of(
                new StudyPlanInfo("top-100-liked", "LeetCode 热题 100", null),
                new StudyPlanInfo("top-interview-150", "面试经典 150 题", null),
                new StudyPlanInfo("coding-interviews", "剑指 Offer", null),
                new StudyPlanInfo("leetcode-75", "LeetCode 75", null),
                new StudyPlanInfo("dynamic-programming", "动态规划（基础版）", null),
                new StudyPlanInfo("dynamic-programming-ii", "动态规划（进阶版）", null),
                new StudyPlanInfo("binary-search", "二分查找 · 系统掌握", null),
                new StudyPlanInfo("algorithm-basic", "编程基础 0 到 1", null),
                new StudyPlanInfo("graph-theory", "图论 · 从入门到精通", null),
                new StudyPlanInfo("pandas-for-data-engineering", "Pandas 入门", null),
                new StudyPlanInfo("sql-basic", "高频 SQL 50 题（基础版）", null),
                new StudyPlanInfo("sql-advanced", "高频 SQL 50 题（进阶版）", null),
                new StudyPlanInfo("front-end-100", "前端面试 100 题", null),
                new StudyPlanInfo("cracking-the-coding-interview", "程序员面试金典", null)
        );
    }

    public List<SearchedProblem> searchProblems(String keyword, int limit) {
        JsonNode data = postGraphQL(
                "problemsetQuestionList",
                """
                query problemsetQuestionList($categorySlug: String, $skip: Int, $limit: Int, $filters: QuestionListFilterInput) {
                  problemsetQuestionList(categorySlug: $categorySlug, skip: $skip, limit: $limit, filters: $filters) {
                    total
                    questions {
                      frontendQuestionId
                      titleSlug
                      title
                      titleCn
                      difficulty
                    }
                  }
                }
                """,
                Map.of(
                        "categorySlug", "all-code-essentials",
                        "skip", 0,
                        "limit", Math.min(Math.max(limit, 1), 50),
                        "filters", Map.of("searchKeywords", keyword == null ? "" : keyword)
                )
        );
        List<SearchedProblem> results = new ArrayList<>();
        data.path("data").path("problemsetQuestionList").path("questions").forEach(q ->
                results.add(new SearchedProblem(
                        q.path("titleSlug").asText(null),
                        q.path("title").asText(null),
                        firstNonBlank(q.path("titleCn").asText(null), q.path("translatedTitle").asText(null)),
                        q.path("difficulty").asText(null),
                        q.path("frontendQuestionId").asText(null)
                ))
        );
        return results;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    public static List<UserProgressQuestion> parseUserProgress(JsonNode data) {
        JsonNode questions = data.path("userProgressQuestionList").path("questions");
        List<UserProgressQuestion> result = new ArrayList<>();
        questions.forEach(q -> result.add(new UserProgressQuestion(
                q.path("frontendId").asText(null),
                q.path("title").asText(null),
                q.path("translatedTitle").asText(null),
                q.path("titleSlug").asText(null),
                q.path("questionStatus").asText(null),
                q.path("lastResult").asText(null),
                parseInstant(q.path("lastSubmittedAt").asText(null))
        )));
        return result;
    }

    public static SubmissionPage parseSubmissionPage(JsonNode data) {
        JsonNode list = data.path("submissionList");
        List<SubmissionItem> items = new ArrayList<>();
        list.path("submissions").forEach(s -> items.add(new SubmissionItem(
                s.path("id").asText(null),
                s.path("title").asText(null),
                s.path("status").asText(null),
                s.path("lang").asText(null),
                s.path("frontendId").asText(null),
                s.path("url").asText(null),
                parseInstant(s.path("timestamp").asText(null))
        )));
        return new SubmissionPage(
                list.path("lastKey").asText(null),
                list.path("hasNext").asBoolean(false),
                items
        );
    }

    public static StudyPlanSummary parseStudyPlan(JsonNode data) {
        JsonNode plan = data.path("studyPlanV2Detail");
        List<StudyPlanQuestion> questions = new ArrayList<>();
        plan.path("planSubGroups").forEach(group ->
                group.path("questions").forEach(q -> questions.add(new StudyPlanQuestion(
                        q.path("titleSlug").asText(null),
                        q.path("translatedTitle").asText(null),
                        q.path("title").asText(null)
                )))
        );
        return new StudyPlanSummary(plan.path("name").asText(null), questions);
    }

    private JsonNode postGraphQL(String operationName, String query, Map<String, Object> variables) {
        Map<String, Object> body = new HashMap<>();
        body.put("operationName", operationName);
        body.put("query", query);
        body.put("variables", variables);

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            throttle();
            try {
                JsonNode response = restClient.post()
                        .uri("")
                        .headers(this::applyHeaders)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(JsonNode.class);
                if (isRateLimitError(response) && attempt < MAX_ATTEMPTS) {
                    sleepBackoff(attempt);
                    continue;
                }
                if (response != null && response.has("errors")) {
                    log.warn("LeetCode GraphQL errors for {}: {}", operationName, response.path("errors"));
                }
                return response;
            } catch (RestClientResponseException ex) {
                if (isRetryableStatus(ex.getStatusCode().value()) && attempt < MAX_ATTEMPTS) {
                    log.warn("LeetCode {} failed with status {}, retrying {}/{}",
                            operationName, ex.getStatusCode().value(), attempt + 1, MAX_ATTEMPTS);
                    sleepBackoff(attempt);
                    continue;
                }
                throw ex;
            } catch (ResourceAccessException ex) {
                if (attempt < MAX_ATTEMPTS) {
                    log.warn("LeetCode {} network error, retrying {}/{}: {}",
                            operationName, attempt + 1, MAX_ATTEMPTS, ex.getMessage());
                    sleepBackoff(attempt);
                    continue;
                }
                throw ex;
            }
        }
        throw new IllegalStateException("LeetCode request failed after retries: " + operationName);
    }

    private boolean isRetryableStatus(int status) {
        return status == 429 || status >= 500;
    }

    private boolean isRateLimitError(JsonNode response) {
        if (response == null || !response.has("errors")) {
            return false;
        }
        String errors = response.path("errors").toString();
        String lower = errors.toLowerCase();
        return lower.contains("rate") || lower.contains("too many") || lower.contains("throttle");
    }

    private void throttle() {
        long now = System.currentTimeMillis();
        long previous = lastRequestAt;
        if (previous != 0 && now - previous < MIN_REQUEST_INTERVAL_MS) {
            try {
                Thread.sleep(MIN_REQUEST_INTERVAL_MS - (now - previous));
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        lastRequestAt = System.currentTimeMillis();
    }

    private void sleepBackoff(int attempt) {
        long delay = 600L * (1L << (attempt - 1));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private void applyHeaders(HttpHeaders headers) {
        headers.set(HttpHeaders.USER_AGENT, BROWSER_USER_AGENT);
        headers.set(HttpHeaders.ORIGIN, ORIGIN);
        headers.set(HttpHeaders.REFERER, ORIGIN + "/");
        if (credentials.getCsrfToken() != null) {
            headers.set("x-csrftoken", credentials.getCsrfToken());
        }
        String cookie = buildCookie();
        if (!cookie.isBlank()) {
            headers.set(HttpHeaders.COOKIE, cookie);
        }
    }

    private String buildCookie() {
        List<String> parts = new ArrayList<>();
        if (credentials.getSession() != null) {
            parts.add("LEETCODE_SESSION=" + credentials.getSession());
        }
        if (credentials.getCsrfToken() != null) {
            parts.add("csrftoken=" + credentials.getCsrfToken());
        }
        if (credentials.getCfClearance() != null) {
            parts.add("cf_clearance=" + credentials.getCfClearance());
        }
        return String.join("; ", parts);
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (Exception e) {
            return null;
        }
    }
}
