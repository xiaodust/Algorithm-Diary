package com.algodiary.service;

import com.algodiary.leetcode.LeetCodeClient;
import com.algodiary.leetcode.StudyPlanSummary;
import com.algodiary.model.ProblemList;
import com.algodiary.model.ProblemState;
import com.algodiary.model.UserGoal;
import com.algodiary.store.AlgoStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import com.algodiary.dto.ListProgress;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProblemListService {

    public static final String SOURCE_CUSTOM = "CUSTOM";
    public static final String SOURCE_IMPORTED = "IMPORTED";

    private final AlgoStore store;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourceLoader;
    private final LeetCodeClient leetCodeClient;

    public ProblemListService(AlgoStore store, ObjectMapper objectMapper,
                              ResourcePatternResolver resourceLoader, LeetCodeClient leetCodeClient) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.leetCodeClient = leetCodeClient;
    }

    @PostConstruct
    public void loadBuiltinLists() throws IOException {
        Resource[] resources = resourceLoader.getResources("classpath:lists/*.json");
        for (Resource resource : resources) {
            ProblemList list = objectMapper.readValue(resource.getInputStream(), ProblemList.class);
            store.saveList(list);
        }
    }

    public List<ProblemList> getAllLists() {
        return store.findAllLists();
    }

    public ProblemList getActiveList() {
        return store.findGoal()
                .flatMap(goal -> store.findList(goal.activeListId()))
                .orElseGet(this::defaultToFirstList);
    }

    public void setActiveList(String listId) {
        ProblemList list = store.findList(listId)
                .orElseThrow(() -> new IllegalArgumentException("题单不存在: " + listId));
        UserGoal current = store.findGoal()
                .orElse(new UserGoal(listId, GoalService.TARGET_COMPLETE_LIST, list.problemSlugs().size(), 3));
        store.saveGoal(new UserGoal(listId, current.targetType(), current.target(), current.dailyTarget()));
    }

    public ProblemList createCustomList(String name, List<String> slugs) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("题单名称不能为空");
        }
        if (trimmedName.length() > 50) {
            throw new IllegalArgumentException("题单名称不能超过 50 个字符");
        }
        List<String> normalized = normalizeSlugs(slugs);
        String id = "custom-" + UUID.randomUUID().toString().substring(0, 8);
        ProblemList list = new ProblemList(id, trimmedName, SOURCE_CUSTOM, normalized);
        store.saveList(list);
        return list;
    }

    public ProblemList updateCustomList(String listId, String name, List<String> slugs) {
        ProblemList existing = store.findList(listId)
                .orElseThrow(() -> new IllegalArgumentException("题单不存在: " + listId));
        if (!isUserOwned(existing.source())) {
            throw new IllegalArgumentException("内置题单不可修改，请新建自定义题单");
        }
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("题单名称不能为空");
        }
        if (trimmedName.length() > 50) {
            throw new IllegalArgumentException("题单名称不能超过 50 个字符");
        }
        ProblemList updated = new ProblemList(listId, trimmedName, SOURCE_CUSTOM, normalizeSlugs(slugs));
        store.saveList(updated);
        return updated;
    }

    public void deleteCustomList(String listId) {
        ProblemList existing = store.findList(listId)
                .orElseThrow(() -> new IllegalArgumentException("题单不存在: " + listId));
        if (!isUserOwned(existing.source())) {
            throw new IllegalArgumentException("内置题单不可删除");
        }
        store.deleteList(listId);
    }

    public ProblemList importStudyPlan(String planSlug) {
        if (planSlug == null || planSlug.isBlank()) {
            throw new IllegalArgumentException("题单标识不能为空");
        }
        StudyPlanSummary summary = leetCodeClient.fetchStudyPlan(planSlug);
        if (summary == null || summary.questions().isEmpty()) {
            throw new IllegalArgumentException("未获取到题单内容: " + planSlug);
        }
        List<String> slugs = summary.questions().stream()
                .map(com.algodiary.leetcode.StudyPlanQuestion::titleSlug)
                .filter(slug -> slug != null && !slug.isBlank())
                .toList();
        String name = summary.name() == null || summary.name().isBlank() ? planSlug : summary.name();
        String id = "custom-" + UUID.randomUUID().toString().substring(0, 8);
        ProblemList list = new ProblemList(id, name, SOURCE_IMPORTED, slugs);
        store.saveList(list);
        return list;
    }

    private List<String> normalizeSlugs(List<String> slugs) {
        if (slugs == null) {
            return List.of();
        }
        return slugs.stream()
                .map(s -> s == null ? "" : s.trim())
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());
    }

    private boolean isUserOwned(String source) {
        return SOURCE_CUSTOM.equals(source) || SOURCE_IMPORTED.equals(source);
    }

    public ListProgress getProgress(ProblemList list) {
        List<ProblemState> states = store.findAllStates();
        int solved = (int) list.problemSlugs().stream()
                .filter(slug -> store.findState(slug)
                        .map(state -> state.acCount() > 0)
                        .orElse(false))
                .count();
        int total = list.problemSlugs().size();
        int remaining = Math.max(0, total - solved);

        LocalDate today = LocalDate.now();
        Instant windowStart = today.minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant();
        long solvedLastSevenDays = states.stream()
                .filter(state -> state.firstAcAt() != null && !state.firstAcAt().isBefore(windowStart))
                .count();
        double pacePerDay = solvedLastSevenDays / 7.0;
        Integer estimatedDays = pacePerDay > 0 ? (int) Math.ceil(remaining / pacePerDay) : null;

        return new ListProgress(list.id(), list.name(), total, solved, remaining, pacePerDay, estimatedDays);
    }

    private ProblemList defaultToFirstList() {
        List<ProblemList> lists = store.findAllLists();
        if (lists.isEmpty()) {
            throw new IllegalStateException("没有可用的题单");
        }
        ProblemList first = lists.getFirst();
        store.saveGoal(new UserGoal(first.id(), GoalService.TARGET_COMPLETE_LIST, first.problemSlugs().size(), 3));
        return first;
    }
}
