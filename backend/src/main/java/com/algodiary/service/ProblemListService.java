package com.algodiary.service;

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

@Service
public class ProblemListService {

    private final AlgoStore store;
    private final ObjectMapper objectMapper;
    private final ResourcePatternResolver resourceLoader;

    public ProblemListService(AlgoStore store, ObjectMapper objectMapper, ResourcePatternResolver resourceLoader) {
        this.store = store;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
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
