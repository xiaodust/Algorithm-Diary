package com.algodiary.service;

import com.algodiary.model.ProblemState;
import com.algodiary.model.Review;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
public class MistakeService {

    private static final Set<String> MISTAKE_STATUSES = Set.of("WA", "TLE", "MLE", "RE", "CE", "OLE");
    private static final int[] INTERVALS_DAYS = {1, 3, 7, 14, 30};

    public boolean isMistakeStatus(String status) {
        return status != null && MISTAKE_STATUSES.contains(status.toUpperCase());
    }

    public String classifyError(String status) {
        if (status == null) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "WA" -> "wrong_answer";
            case "TLE" -> "timeout";
            case "MLE" -> "memory_limit";
            case "RE" -> "runtime_error";
            case "CE" -> "compile_error";
            default -> null;
        };
    }

    public Instant nextReview(ProblemState state, boolean passed, Instant now) {
        if (!passed) {
            return now.plus(Duration.ofDays(1));
        }
        int index = Math.min(state.reviewCount(), INTERVALS_DAYS.length - 1);
        return now.plus(Duration.ofDays(INTERVALS_DAYS[index]));
    }

    public boolean shouldGraduate(List<Review> reviews) {
        if (reviews == null || reviews.size() < 2) {
            return false;
        }
        List<Review> sorted = reviews.stream()
                .sorted(Comparator.comparing(Review::reviewedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return sorted.get(sorted.size() - 1).passed() && sorted.get(sorted.size() - 2).passed();
    }
}
