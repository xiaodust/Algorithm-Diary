package com.algodiary.service;

import com.algodiary.model.Problem;
import com.algodiary.model.ProblemList;
import com.algodiary.model.ProblemState;
import org.springframework.stereotype.Service;
import com.algodiary.dto.Recommendation;

import java.util.*;

@Service
public class RecommendationService {

    public List<Recommendation> recommend(
            ProblemList activeList,
            List<Problem> allProblems,
            List<ProblemState> states,
            Set<String> weakTopicIds,
            int limit
    ) {
        Map<String, ProblemState> stateBySlug = new HashMap<>();
        for (ProblemState state : states) {
            stateBySlug.put(state.problemSlug(), state);
        }

        Map<String, Problem> problemBySlug = new HashMap<>();
        for (Problem problem : allProblems) {
            problemBySlug.put(problem.slug(), problem);
        }

        List<Recommendation> result = new ArrayList<>();
        Set<String> emitted = new HashSet<>();

        for (String slug : activeList.problemSlugs()) {
            if (result.size() >= limit) {
                break;
            }
            Problem problem = problemBySlug.get(slug);
            if (isCandidate(problem, stateBySlug.get(slug), weakTopicIds) && emitted.add(slug)) {
                result.add(toRecommendation(problem));
            }
        }

        for (Problem problem : allProblems) {
            if (result.size() >= limit) {
                break;
            }
            if (!emitted.contains(problem.slug())
                    && isCandidate(problem, stateBySlug.get(problem.slug()), weakTopicIds)) {
                result.add(toRecommendation(problem));
            }
        }

        return result;
    }

    private boolean isCandidate(Problem problem, ProblemState state, Set<String> weakTopicIds) {
        if (problem == null) {
            return false;
        }
        boolean unsolved = state == null || state.acCount() == 0;
        boolean weakTopic = problem.topics() != null
                && problem.topics().stream().anyMatch(weakTopicIds::contains);
        return unsolved && weakTopic;
    }

    private Recommendation toRecommendation(Problem problem) {
        String reason = problem.topics() == null || problem.topics().isEmpty()
                ? "薄弱题型"
                : "薄弱题型: " + String.join("、", problem.topics());
        return new Recommendation(
                problem.slug(),
                reason,
                "https://leetcode.cn/problems/" + problem.slug() + "/"
        );
    }
}
