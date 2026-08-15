package com.algodiary.support;

import com.algodiary.model.*;
import com.algodiary.store.AlgoStore;
import com.algodiary.dto.Recommendation;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

public class InMemoryAlgoStore implements AlgoStore {

    private final Map<String, Problem> problems = new LinkedHashMap<>();
    private final Map<String, List<Submission>> submissions = new LinkedHashMap<>();
    private final Map<String, ProblemState> states = new LinkedHashMap<>();
    private final Map<String, ProblemList> lists = new LinkedHashMap<>();
    private final Map<String, Topic> topics = new LinkedHashMap<>();
    private final Map<String, List<String>> problemTopics = new LinkedHashMap<>();
    private final Map<String, MistakeNote> mistakes = new LinkedHashMap<>();
    private final Map<String, List<Review>> reviews = new LinkedHashMap<>();
    private final Map<LocalDate, DailyPlan> plans = new HashMap<>();
    private final Map<String, String> insights = new LinkedHashMap<>();
    private final List<Recommendation> recommendations = new ArrayList<>();
    private String agentMemory;
    private UserGoal goal;

    @Override
    public void saveProblem(Problem problem) {
        problems.put(problem.slug(), problem);
    }

    @Override
    public Optional<Problem> findProblem(String slug) {
        return Optional.ofNullable(problems.get(slug));
    }

    @Override
    public List<Problem> findAllProblems() {
        return new ArrayList<>(problems.values());
    }

    @Override
    public void saveSubmission(Submission submission) {
        submissions.computeIfAbsent(submission.problemSlug(), k -> new ArrayList<>()).add(submission);
    }

    @Override
    public List<Submission> findSubmissions(String problemSlug) {
        return new ArrayList<>(submissions.getOrDefault(problemSlug, List.of()));
    }

    @Override
    public List<Submission> findAllSubmissions() {
        return submissions.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public void saveState(ProblemState state) {
        states.put(state.problemSlug(), state);
    }

    @Override
    public Optional<ProblemState> findState(String problemSlug) {
        return Optional.ofNullable(states.get(problemSlug));
    }

    @Override
    public List<ProblemState> findAllStates() {
        return new ArrayList<>(states.values());
    }

    @Override
    public void saveList(ProblemList problemList) {
        lists.put(problemList.id(), problemList);
    }

    @Override
    public Optional<ProblemList> findList(String listId) {
        return Optional.ofNullable(lists.get(listId));
    }

    @Override
    public List<ProblemList> findAllLists() {
        return new ArrayList<>(lists.values());
    }

    @Override
    public void saveTopic(Topic topic) {
        topics.put(topic.id(), topic);
    }

    @Override
    public List<Topic> findAllTopics() {
        return new ArrayList<>(topics.values());
    }

    @Override
    public void saveProblemTopic(String problemSlug, String topicId) {
        problemTopics.computeIfAbsent(problemSlug, k -> new ArrayList<>()).add(topicId);
    }

    @Override
    public List<String> findTopics(String problemSlug) {
        return new ArrayList<>(problemTopics.getOrDefault(problemSlug, List.of()));
    }

    @Override
    public void saveMistake(MistakeNote note) {
        mistakes.put(note.problemSlug(), note);
    }

    @Override
    public void deleteMistake(String problemSlug) {
        mistakes.remove(problemSlug);
    }

    @Override
    public Optional<MistakeNote> findMistake(String problemSlug) {
        return Optional.ofNullable(mistakes.get(problemSlug));
    }

    @Override
    public List<MistakeNote> findAllMistakes() {
        return new ArrayList<>(mistakes.values());
    }

    @Override
    public void saveReview(Review review) {
        reviews.computeIfAbsent(review.problemSlug(), k -> new ArrayList<>()).add(review);
    }

    @Override
    public List<Review> findReviews(String problemSlug) {
        return new ArrayList<>(reviews.getOrDefault(problemSlug, List.of()));
    }

    @Override
    public List<Review> findAllReviews() {
        return reviews.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public void savePlan(DailyPlan plan) {
        plans.put(plan.date(), plan);
    }

    @Override
    public Optional<DailyPlan> findPlan(LocalDate date) {
        return Optional.ofNullable(plans.get(date));
    }

    @Override
    public List<LocalDate> findCompletedPlanDates() {
        return plans.values().stream()
                .filter(DailyPlan::completed)
                .map(DailyPlan::date)
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    @Override
    public void saveGoal(UserGoal goal) {
        this.goal = goal;
    }

    @Override
    public Optional<UserGoal> findGoal() {
        return Optional.ofNullable(goal);
    }

    @Override
    public void saveInsight(String type, String content) {
        insights.put(type, content);
    }

    @Override
    public Optional<String> findLatestInsight(String type) {
        return Optional.ofNullable(insights.get(type));
    }

    @Override
    public void saveRecommendation(Recommendation recommendation) {
        recommendations.add(recommendation);
    }

    @Override
    public List<Recommendation> findRecommendationsSince(Instant since) {
        return new ArrayList<>(recommendations);
    }

    @Override
    public void saveAgentMemory(String content) {
        this.agentMemory = content;
    }

    @Override
    public Optional<String> findAgentMemory() {
        return Optional.ofNullable(agentMemory);
    }

    @Override
    public void clearPracticeData() {
        problems.clear();
        submissions.clear();
        states.clear();
        mistakes.clear();
        reviews.clear();
        plans.clear();
        problemTopics.clear();
        insights.clear();
        recommendations.clear();
        agentMemory = null;
    }
}
