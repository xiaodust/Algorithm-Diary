package com.algodiary.service;

import com.algodiary.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Predicate;

@Service
public class PlannerService {

    public DailyPlan plan(
            ProblemList activeList,
            List<Problem> problems,
            List<ProblemState> states,
            List<String> mistakeSlugs,
            Set<String> weakTopicIds,
            Instant now
    ) {
        Map<String, Problem> problemBySlug = indexProblems(problems);
        Map<String, ProblemState> stateBySlug = indexStates(states);
        Set<String> mistakeSet = new HashSet<>(mistakeSlugs);

        List<PlanTask> listMistakes = new ArrayList<>();
        List<PlanTask> listReviews = new ArrayList<>();
        List<PlanTask> listWeak = new ArrayList<>();
        List<PlanTask> listNew = new ArrayList<>();
        Set<String> used = new HashSet<>();

        for (String slug : activeList.problemSlugs()) {
            if (!used.add(slug)) {
                continue;
            }
            Problem problem = problemBySlug.get(slug);
            ProblemState state = stateBySlug.get(slug);
            PlanTask task = classifyListTask(slug, problem, state, mistakeSet, weakTopicIds, now);
            if (task == null) {
                continue;
            }
            switch (task.reason()) {
                case MISTAKE -> listMistakes.add(task);
                case REVIEW -> listReviews.add(task);
                case WEAK_TOPIC -> listWeak.add(task);
                case LIST_NEW -> listNew.add(task);
                default -> { }
            }
        }

        List<PlanTask> ordered = new ArrayList<>();
        ordered.addAll(listMistakes);
        ordered.addAll(listReviews);
        ordered.addAll(listWeak);
        ordered.addAll(listNew);

        appendOutsideFallback(ordered, used, activeList.problemSlugs(), stateBySlug, mistakeSet, now);

        int coreSize = Math.min(2, ordered.size());
        List<PlanTask> core = List.copyOf(ordered.subList(0, coreSize));
        int bonusSize = Math.min(2, ordered.size() - coreSize);
        List<PlanTask> bonus = List.copyOf(ordered.subList(coreSize, coreSize + bonusSize));

        return new DailyPlan(now.atZone(ZoneId.systemDefault()).toLocalDate(), core, bonus, false);
    }

    private PlanTask classifyListTask(
            String slug,
            Problem problem,
            ProblemState state,
            Set<String> mistakes,
            Set<String> weakTopics,
            Instant now
    ) {
        if (mistakes.contains(slug)) {
            return new PlanTask(slug, TaskReason.MISTAKE);
        }
        if (state != null && state.nextReviewAt() != null && !now.isBefore(state.nextReviewAt())) {
            return new PlanTask(slug, TaskReason.REVIEW);
        }
        if (isUnsolved(state)) {
            if (problem != null && problem.topics() != null
                    && problem.topics().stream().anyMatch(weakTopics::contains)) {
                return new PlanTask(slug, TaskReason.WEAK_TOPIC);
            }
            return new PlanTask(slug, TaskReason.LIST_NEW);
        }
        return null;
    }

    private void appendOutsideFallback(
            List<PlanTask> ordered,
            Set<String> used,
            List<String> listSlugs,
            Map<String, ProblemState> stateBySlug,
            Set<String> mistakes,
            Instant now
    ) {
        Set<String> listSet = new HashSet<>(listSlugs);
        List<PlanTask> outsideMistakes = new ArrayList<>();
        List<PlanTask> outsideReviews = new ArrayList<>();
        for (Map.Entry<String, ProblemState> entry : stateBySlug.entrySet()) {
            String slug = entry.getKey();
            if (listSet.contains(slug) || !used.add(slug)) {
                continue;
            }
            ProblemState state = entry.getValue();
            if (mistakes.contains(slug)) {
                outsideMistakes.add(new PlanTask(slug, TaskReason.MISTAKE));
            } else if (state.nextReviewAt() != null && !now.isBefore(state.nextReviewAt())) {
                outsideReviews.add(new PlanTask(slug, TaskReason.REVIEW));
            }
        }
        ordered.addAll(outsideMistakes);
        ordered.addAll(outsideReviews);
    }

    private boolean isUnsolved(ProblemState state) {
        return state == null || state.acCount() == 0;
    }

    private Map<String, Problem> indexProblems(List<Problem> problems) {
        Map<String, Problem> map = new HashMap<>();
        for (Problem problem : problems) {
            map.put(problem.slug(), problem);
        }
        return map;
    }

    private Map<String, ProblemState> indexStates(List<ProblemState> states) {
        Map<String, ProblemState> map = new HashMap<>();
        for (ProblemState state : states) {
            map.put(state.problemSlug(), state);
        }
        return map;
    }
}
