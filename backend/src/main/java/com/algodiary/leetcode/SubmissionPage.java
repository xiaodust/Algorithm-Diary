package com.algodiary.leetcode;

import java.util.List;

public record SubmissionPage(
        String lastKey,
        boolean hasNext,
        List<SubmissionItem> items
) {
}
