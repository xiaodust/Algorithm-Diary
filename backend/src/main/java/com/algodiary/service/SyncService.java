package com.algodiary.service;

import com.algodiary.leetcode.LeetCodeClient;
import com.algodiary.leetcode.SubmissionItem;
import com.algodiary.leetcode.SubmissionPage;
import com.algodiary.leetcode.UserProgressQuestion;
import com.algodiary.model.*;
import com.algodiary.store.AlgoStore;
import org.springframework.stereotype.Service;
import com.algodiary.dto.SyncResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

@Service
public class SyncService {

    private static final Logger log = LoggerFactory.getLogger(SyncService.class);
    private static final int PAGE_SIZE = 100;
    private static final int MAX_PAGES = 20;

    private final LeetCodeClient client;
    private final AlgoStore store;
    private final MistakeService mistakeService;
    private final ProblemTitleService titleService;

    public SyncService(LeetCodeClient client, AlgoStore store, MistakeService mistakeService, ProblemTitleService titleService) {
        this.client = client;
        this.store = store;
        this.mistakeService = mistakeService;
        this.titleService = titleService;
    }

    public SyncResult sync() {
        Instant now = Instant.now();
        List<UserProgressQuestion> progress = client.fetchUserProgress();
        List<SubmissionItem> items = List.of();
        try {
            items = fetchAllSubmissions();
        } catch (Exception e) {
            log.warn("获取提交明细失败，将仅使用已做题目进度同步", e);
        }

        Map<String, List<Submission>> submissionsBySlug = new LinkedHashMap<>();
        for (SubmissionItem item : items) {
            String slug = inferSlug(item);
            if (slug == null) {
                continue;
            }
            store.saveProblem(Problem.withDefaults(slug, item.title(), null));
            Submission submission = new Submission(slug, item.status(), item.lang(), item.timestamp());
            store.saveSubmission(submission);
            submissionsBySlug.computeIfAbsent(slug, k -> new ArrayList<>()).add(submission);
        }

        for (UserProgressQuestion question : progress) {
            if (question.titleSlug() == null || question.titleSlug().isBlank()) {
                continue;
            }
            store.saveProblem(Problem.withDefaults(question.titleSlug(), question.title(), null));
            titleService.saveTitle(question.titleSlug(), question.translatedTitle());
        }

        for (UserProgressQuestion question : progress) {
            if (question.titleSlug() == null || question.titleSlug().isBlank()) {
                continue;
            }
            List<Submission> submissions = submissionsBySlug.getOrDefault(question.titleSlug(), List.of());
            ProblemState state = stateFromProgress(question, submissions, now);
            store.saveState(state);
        }

        for (Map.Entry<String, List<Submission>> entry : submissionsBySlug.entrySet()) {
            if (progress.stream().anyMatch(q -> entry.getKey().equals(q.titleSlug()))) {
                continue;
            }
            ProblemState state = buildState(entry.getKey(), entry.getValue(), now);
            store.saveState(state);
        }

        return new SyncResult(progress.size() + items.size(), items.size());
    }

    public ProblemState buildState(String slug, List<Submission> submissions, Instant now) {
        if (submissions == null || submissions.isEmpty()) {
            return ProblemState.empty(slug);
        }

        List<Submission> sorted = submissions.stream()
                .sorted(Comparator.comparing(Submission::submittedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        int acCount = (int) sorted.stream().filter(Submission::isAccepted).count();
        Submission latest = sorted.getLast();
        boolean mistake = mistakeService.isMistakeStatus(latest.status());
        String mistakeType = mistake ? mistakeService.classifyError(latest.status()) : null;
        Instant firstAc = sorted.stream()
                .filter(Submission::isAccepted)
                .map(Submission::submittedAt)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);

        return new ProblemState(
                slug,
                acCount > 0 ? 1 : 0,
                acCount,
                sorted.size(),
                mistake,
                mistakeType,
                null,
                null,
                0,
                firstAc
        );
    }

    private ProblemState stateFromProgress(UserProgressQuestion question, List<Submission> submissions, Instant now) {
        boolean solved = "SOLVED".equalsIgnoreCase(question.questionStatus())
                || "AC".equalsIgnoreCase(question.lastResult());
        int acCount = (int) submissions.stream().filter(Submission::isAccepted).count();
        if (solved && acCount == 0) {
            acCount = 1;
        }
        int attemptCount = submissions.isEmpty() ? (solved ? 1 : 0) : submissions.size();
        boolean mistake = !solved && mistakeService.isMistakeStatus(question.lastResult());
        String mistakeType = mistake ? mistakeService.classifyError(question.lastResult()) : null;

        return new ProblemState(
                question.titleSlug(),
                solved ? 1 : 0,
                acCount,
                attemptCount,
                mistake,
                mistakeType,
                null,
                null,
                0,
                solved ? question.lastSubmittedAt() : null
        );
    }

    private List<SubmissionItem> fetchAllSubmissions() {
        List<SubmissionItem> all = new ArrayList<>();
        String lastKey = null;
        for (int page = 0; page < MAX_PAGES; page++) {
            SubmissionPage submissionPage = client.fetchSubmissionsPage(page * PAGE_SIZE, PAGE_SIZE, lastKey);
            all.addAll(submissionPage.items());
            if (!submissionPage.hasNext()) {
                break;
            }
            lastKey = submissionPage.lastKey();
        }
        return all;
    }

    private String inferSlug(SubmissionItem item) {
        if (item.url() != null) {
            int start = item.url().indexOf("/problems/");
            if (start >= 0) {
                String rest = item.url().substring(start + "/problems/".length());
                int end = rest.indexOf('/');
                if (end > 0) {
                    return rest.substring(0, end);
                }
            }
        }
        return item.title() == null ? null : item.title().toLowerCase().replace(' ', '-');
    }
}
