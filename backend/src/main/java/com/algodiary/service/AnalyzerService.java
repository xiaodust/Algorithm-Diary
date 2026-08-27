package com.algodiary.service;

import com.algodiary.model.Problem;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Review;
import org.springframework.stereotype.Service;
import com.algodiary.dto.TopicStats;

import java.util.*;

@Service
public class AnalyzerService {

    public List<TopicStats> analyze(
            List<Problem> problems,
            List<ProblemState> states,
            List<Review> reviews,
            Set<String> topicIds,
            int minSample
    ) {
        Map<String, ProblemState> stateBySlug = new HashMap<>();
        for (ProblemState state : states) {
            stateBySlug.put(state.problemSlug(), state);
        }

        Map<String, List<Review>> reviewsBySlug = new HashMap<>();
        for (Review review : reviews) {
            reviewsBySlug.computeIfAbsent(review.problemSlug(), k -> new ArrayList<>()).add(review);
        }

        Map<String, List<Problem>> problemsByTopic = new HashMap<>();
        for (Problem problem : problems) {
            if (problem.topics() == null) {
                continue;
            }
            for (String topic : problem.topics()) {
                problemsByTopic.computeIfAbsent(topic, k -> new ArrayList<>()).add(problem);
            }
        }

        List<TopicStats> result = new ArrayList<>();
        for (String topicId : topicIds) {
            List<Problem> topicProblems = problemsByTopic.getOrDefault(topicId, List.of());
            if (topicProblems.size() < minSample) {
                continue;
            }

            int solved = 0;
            int attemptsSum = 0;
            int masterySum = 0;
            int failedReviews = 0;
            int totalReviews = 0;

            for (Problem problem : topicProblems) {
                ProblemState state = stateBySlug.get(problem.slug());
                if (state == null) {
                    continue;
                }
                if (state.acCount() > 0) {
                    solved++;
                }
                attemptsSum += state.attemptCount();
                masterySum += state.masteryLevel();
                for (Review review : reviewsBySlug.getOrDefault(problem.slug(), List.of())) {
                    totalReviews++;
                    if (!review.passed()) {
                        failedReviews++;
                    }
                }
            }

            int count = topicProblems.size();
            double acRate = (double) solved / count;
            double avgAttempts = (double) attemptsSum / count;
            double masteryAvg = (double) masterySum / count;
            double forgetRate = totalReviews == 0 ? 0.0 : (double) failedReviews / totalReviews;

            boolean weak = acRate < 0.6 && (avgAttempts >= 2.0 || forgetRate > 0.3);
            boolean strong = acRate >= 0.8 && masteryAvg >= 2.0 && forgetRate <= 0.2;

            result.add(new TopicStats(topicId, count, acRate, avgAttempts, masteryAvg, forgetRate, weak, strong));
        }

        result.sort(Comparator.comparing(TopicStats::topicId));
        return result;
    }
}
