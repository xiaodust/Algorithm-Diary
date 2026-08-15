package com.algodiary.service;

import com.algodiary.dto.MemoryProfile;
import com.algodiary.dto.Recommendation;
import com.algodiary.dto.TopicStats;
import com.algodiary.llm.LlmGateway;
import com.algodiary.model.Problem;
import com.algodiary.model.ProblemList;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Topic;
import com.algodiary.store.AlgoStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentRecommendationService {

    private static final TypeReference<List<AiRecommendation>> AI_RECOMMENDATION_TYPE = new TypeReference<>() {
    };

    private final RecommendationService recommendationService;
    private final AgentMemoryService memoryService;
    private final AlgoStore store;
    private final ProblemListService listService;
    private final AnalyzerService analyzer;
    private final LlmGateway llm;
    private final ObjectMapper objectMapper;

    public AgentRecommendationService(
            RecommendationService recommendationService,
            AgentMemoryService memoryService,
            AlgoStore store,
            ProblemListService listService,
            AnalyzerService analyzer,
            LlmGateway llm,
            ObjectMapper objectMapper
    ) {
        this.recommendationService = recommendationService;
        this.memoryService = memoryService;
        this.store = store;
        this.listService = listService;
        this.analyzer = analyzer;
        this.llm = llm;
        this.objectMapper = objectMapper;
    }

    public List<Recommendation> recommend(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        LocalDate today = LocalDate.now(ZoneId.systemDefault());
        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        List<Recommendation> cached = store.findRecommendationsSince(todayStart);
        if (cached.size() >= safeLimit) {
            return cached.stream().limit(safeLimit).toList();
        }

        List<Recommendation> generated = generate(safeLimit);
        generated.forEach(store::saveRecommendation);
        return generated;
    }

    private List<Recommendation> generate(int limit) {
        ProblemList active = listService.getActiveList();
        List<Problem> problems = store.findAllProblems();
        List<ProblemState> states = store.findAllStates();
        Set<String> topicIds = store.findAllTopics().stream()
                .map(Topic::id)
                .collect(Collectors.toSet());
        Set<String> weakTopics = analyzer.analyze(problems, states, List.of(), topicIds, 1).stream()
                .filter(TopicStats::weak)
                .map(TopicStats::topicId)
                .collect(Collectors.toSet());

        List<Recommendation> candidates = recommendationService.recommend(
                active,
                problems,
                states,
                weakTopics,
                Math.min(10, limit)
        );
        if (candidates.isEmpty()) {
            return List.of();
        }

        if (llm != null && llm.isConfigured()) {
            List<Recommendation> aiRecommendations = aiRecommendations(candidates);
            if (!aiRecommendations.isEmpty()) {
                return aiRecommendations.stream().limit(limit).toList();
            }
        }
        return candidates.stream().limit(limit).toList();
    }

    private List<Recommendation> aiRecommendations(List<Recommendation> candidates) {
        MemoryProfile profile = memoryService.refreshProfile();
        String system = "你是算法刷题教练，请根据用户长期学习画像，从候选题中挑选最值得做的题，并给出简短原因。"
                + "只输出 JSON 数组，不要输出 Markdown。";
        String user = "用户画像：\n" + profile + "\n\n候选题：\n" + candidates
                + "\n\n返回格式：[{\"slug\":\"题目slug\",\"reason\":\"推荐原因\"}]";
        try {
            String content = llm.complete(system, user);
            List<AiRecommendation> parsed = parseAiRecommendations(content);
            Set<String> allowedSlugs = candidates.stream()
                    .map(Recommendation::problemSlug)
                    .collect(Collectors.toSet());
            return parsed.stream()
                    .filter(item -> item.slug() != null && allowedSlugs.contains(item.slug()))
                    .map(item -> new Recommendation(
                            item.slug(),
                            item.reason() == null || item.reason().isBlank() ? "根据你的薄弱点推荐" : item.reason(),
                            "https://leetcode.cn/problems/" + item.slug() + "/"
                    ))
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<AiRecommendation> parseAiRecommendations(String content) throws Exception {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        String json = extractJsonArray(content);
        return objectMapper.readValue(json, AI_RECOMMENDATION_TYPE);
    }

    private String extractJsonArray(String content) {
        String trimmed = content.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start < 0 || end < start) {
            return "[]";
        }
        return trimmed.substring(start, end + 1);
    }

    private record AiRecommendation(String slug, String reason) {
    }
}
