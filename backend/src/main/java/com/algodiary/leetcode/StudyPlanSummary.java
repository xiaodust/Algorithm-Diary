package com.algodiary.leetcode;

import java.util.List;

public record StudyPlanSummary(
        String name,
        List<StudyPlanQuestion> questions
) {
}
