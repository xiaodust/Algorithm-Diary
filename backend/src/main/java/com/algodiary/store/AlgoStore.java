package com.algodiary.store;

import com.algodiary.model.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AlgoStore {

    void saveProblem(Problem problem);
    Optional<Problem> findProblem(String slug);
    List<Problem> findAllProblems();

    void saveSubmission(Submission submission);
    List<Submission> findSubmissions(String problemSlug);

    void saveState(ProblemState state);
    Optional<ProblemState> findState(String problemSlug);
    List<ProblemState> findAllStates();

    void saveList(ProblemList problemList);
    Optional<ProblemList> findList(String listId);
    List<ProblemList> findAllLists();

    void saveTopic(Topic topic);
    List<Topic> findAllTopics();

    void saveProblemTopic(String problemSlug, String topicId);
    List<String> findTopics(String problemSlug);

    void saveMistake(MistakeNote note);
    Optional<MistakeNote> findMistake(String problemSlug);
    List<MistakeNote> findAllMistakes();

    void saveReview(Review review);
    List<Review> findReviews(String problemSlug);

    void savePlan(DailyPlan plan);
    Optional<DailyPlan> findPlan(LocalDate date);
    List<LocalDate> findCompletedPlanDates();

    void saveGoal(UserGoal goal);
    Optional<UserGoal> findGoal();

    void deleteMistake(String problemSlug);

    void saveInsight(String type, String content);
    Optional<String> findLatestInsight(String type);

    void clearPracticeData();
}
