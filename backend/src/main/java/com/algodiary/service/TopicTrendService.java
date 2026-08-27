package com.algodiary.service;

import com.algodiary.dto.TopicTrendPoint;
import com.algodiary.model.Problem;
import com.algodiary.model.Review;
import com.algodiary.model.Submission;
import com.algodiary.store.AlgoStore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TopicTrendService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_DAYS = 365;

    private final AlgoStore store;

    public TopicTrendService(AlgoStore store) {
        this.store = store;
    }

    public List<TopicTrendPoint> trend(String topicId, Integer days) {
        int windowDays = days == null ? DEFAULT_DAYS : Math.max(1, Math.min(days, MAX_DAYS));
        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);
        LocalDate start = today.minusDays(windowDays - 1L);

        Set<String> slugs = store.findAllProblems().stream()
                .filter(problem -> problem.topics() != null && problem.topics().contains(topicId))
                .map(Problem::slug)
                .collect(java.util.stream.Collectors.toSet());
        if (slugs.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, List<Submission>> submissionsByDate = new HashMap<>();
        for (Submission submission : store.findAllSubmissions()) {
            if (!slugs.contains(submission.problemSlug()) || submission.submittedAt() == null) {
                continue;
            }
            LocalDate date = toLocalDate(submission.submittedAt(), zone);
            submissionsByDate.computeIfAbsent(date, key -> new ArrayList<>()).add(submission);
        }

        Map<LocalDate, List<Review>> reviewsByDate = new HashMap<>();
        for (Review review : store.findAllReviews()) {
            if (!slugs.contains(review.problemSlug()) || review.reviewedAt() == null) {
                continue;
            }
            LocalDate date = toLocalDate(review.reviewedAt(), zone);
            reviewsByDate.computeIfAbsent(date, key -> new ArrayList<>()).add(review);
        }

        Set<String> solvedSlugs = new HashSet<>();
        int totalAttempts = 0;
        int totalReviews = 0;
        int failedReviews = 0;
        List<TopicTrendPoint> result = new ArrayList<>();

        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            for (Submission submission : submissionsByDate.getOrDefault(date, List.of())) {
                totalAttempts++;
                if (submission.isAccepted()) {
                    solvedSlugs.add(submission.problemSlug());
                }
            }
            for (Review review : reviewsByDate.getOrDefault(date, List.of())) {
                totalReviews++;
                if (!review.passed()) {
                    failedReviews++;
                }
            }

            int solved = solvedSlugs.size();
            double acRate = slugs.isEmpty() ? 0.0 : (double) solved / slugs.size();
            double forgetRate = totalReviews == 0 ? 0.0 : (double) failedReviews / totalReviews;
            result.add(new TopicTrendPoint(date, solved, totalAttempts, acRate, forgetRate));
        }

        return result;
    }

    private LocalDate toLocalDate(Instant instant, ZoneId zone) {
        return instant.atZone(zone).toLocalDate();
    }
}
