package com.algodiary.service;

import com.algodiary.dto.ListProgress;
import com.algodiary.dto.MemoryProfile;
import com.algodiary.dto.TopicStats;
import com.algodiary.model.MistakeNote;
import com.algodiary.model.Problem;
import com.algodiary.model.ProblemList;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Topic;
import com.algodiary.store.AlgoStore;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AgentMemoryService {

    private static final TypeReference<MemoryProfile> PROFILE_TYPE = new TypeReference<>() {
    };

    private final AlgoStore store;
    private final AnalyzerService analyzer;
    private final TopicService topicService;
    private final ProblemListService listService;
    private final GoalService goalService;
    private final ObjectMapper objectMapper;

    public AgentMemoryService(
            AlgoStore store,
            AnalyzerService analyzer,
            TopicService topicService,
            ProblemListService listService,
            GoalService goalService,
            ObjectMapper objectMapper
    ) {
        this.store = store;
        this.analyzer = analyzer;
        this.topicService = topicService;
        this.listService = listService;
        this.goalService = goalService;
        this.objectMapper = objectMapper;
    }

    public MemoryProfile getProfile() {
        return store.findAgentMemory()
                .flatMap(this::parseProfile)
                .orElseGet(this::refreshProfile);
    }

    public MemoryProfile refreshProfile() {
        MemoryProfile profile = buildProfile();
        store.saveAgentMemory(toJson(profile));
        return profile;
    }

    private MemoryProfile buildProfile() {
        List<Problem> problems = store.findAllProblems();
        List<ProblemState> states = store.findAllStates();
        List<MistakeNote> mistakes = store.findAllMistakes();
        ProblemList active = listService.getActiveList();
        ListProgress progress = listService.getProgress(active);
        Set<String> topicIds = topicService.findAllTopics().stream()
                .map(Topic::id)
                .collect(Collectors.toSet());
        List<TopicStats> stats = analyzer.analyze(problems, states, List.of(), topicIds, 1);

        List<String> weakTopics = stats.stream()
                .filter(TopicStats::weak)
                .sorted(Comparator.comparingDouble(TopicStats::acRate))
                .map(TopicStats::topicId)
                .toList();
        List<String> strongTopics = stats.stream()
                .filter(TopicStats::strong)
                .sorted(Comparator.comparingDouble(TopicStats::acRate).reversed())
                .map(TopicStats::topicId)
                .toList();
        List<String> recentMistakeSlugs = mistakes.stream()
                .map(MistakeNote::problemSlug)
                .limit(8)
                .toList();
        int solvedCount = (int) states.stream()
                .filter(state -> state.acCount() > 0)
                .count();

        return new MemoryProfile(
                Instant.now().toString(),
                active.id(),
                solvedCount,
                mistakes.size(),
                goalService.getDailyTarget(),
                progress.pacePerDay(),
                weakTopics,
                strongTopics,
                recentMistakeSlugs
        );
    }

    private String toJson(MemoryProfile profile) {
        try {
            return objectMapper.writeValueAsString(profile);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to serialize agent memory", ex);
        }
    }

    private java.util.Optional<MemoryProfile> parseProfile(String json) {
        try {
            return java.util.Optional.of(objectMapper.readValue(json, PROFILE_TYPE));
        } catch (Exception ex) {
            return java.util.Optional.empty();
        }
    }
}
