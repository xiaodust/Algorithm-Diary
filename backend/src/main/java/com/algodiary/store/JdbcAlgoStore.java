package com.algodiary.store;

import com.algodiary.model.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JdbcAlgoStore implements AlgoStore {

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public JdbcAlgoStore(JdbcTemplate jdbc, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    public void saveProblem(Problem problem) {
        jdbc.update(
                "INSERT OR REPLACE INTO problems(slug, title, difficulty, tags_json, topics_json) VALUES (?, ?, ?, ?, ?)",
                problem.slug(),
                problem.title(),
                problem.difficulty() == null ? null : problem.difficulty().name(),
                toJson(problem.tags()),
                toJson(problem.topics())
        );
    }

    @Override
    public Optional<Problem> findProblem(String slug) {
        List<Problem> problems = jdbc.query(
                "SELECT slug, title, difficulty, tags_json, topics_json FROM problems WHERE slug = ?",
                problemRowMapper(),
                slug
        );
        return problems.stream().findFirst();
    }

    @Override
    public List<Problem> findAllProblems() {
        return jdbc.query(
                "SELECT slug, title, difficulty, tags_json, topics_json FROM problems ORDER BY slug",
                problemRowMapper()
        );
    }

    @Override
    public void saveSubmission(Submission submission) {
        jdbc.update(
                "INSERT INTO submissions(problem_slug, status, lang, submitted_at) VALUES (?, ?, ?, ?)",
                submission.problemSlug(),
                submission.status(),
                submission.lang(),
                submission.submittedAt() == null ? null : submission.submittedAt().toString()
        );
    }

    @Override
    public List<Submission> findSubmissions(String problemSlug) {
        return jdbc.query(
                "SELECT problem_slug, status, lang, submitted_at FROM submissions WHERE problem_slug = ? ORDER BY submitted_at DESC",
                (rs, rowNum) -> new Submission(
                        rs.getString("problem_slug"),
                        rs.getString("status"),
                        rs.getString("lang"),
                        parseInstant(rs.getString("submitted_at"))
                ),
                problemSlug
        );
    }

    @Override
    public void saveState(ProblemState state) {
        jdbc.update(
                "INSERT OR REPLACE INTO problem_states(problem_slug, mastery_level, ac_count, attempt_count, is_mistake, mistake_type, last_review_at, next_review_at, review_count, first_ac_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                state.problemSlug(),
                state.masteryLevel(),
                state.acCount(),
                state.attemptCount(),
                state.mistake() ? 1 : 0,
                state.mistakeType(),
                toInstantString(state.lastReviewAt()),
                toInstantString(state.nextReviewAt()),
                state.reviewCount(),
                toInstantString(state.firstAcAt())
        );
    }

    @Override
    public Optional<ProblemState> findState(String problemSlug) {
        List<ProblemState> states = jdbc.query(
                "SELECT * FROM problem_states WHERE problem_slug = ?",
                stateRowMapper(),
                problemSlug
        );
        return states.stream().findFirst();
    }

    @Override
    public List<ProblemState> findAllStates() {
        return jdbc.query("SELECT * FROM problem_states", stateRowMapper());
    }

    @Override
    public void saveList(ProblemList problemList) {
        jdbc.update(
                "INSERT OR REPLACE INTO problem_lists(id, name, source) VALUES (?, ?, ?)",
                problemList.id(),
                problemList.name(),
                problemList.source()
        );
        jdbc.update("DELETE FROM problem_list_items WHERE list_id = ?", problemList.id());
        List<String> slugs = problemList.problemSlugs();
        for (int i = 0; i < slugs.size(); i++) {
            jdbc.update(
                    "INSERT INTO problem_list_items(list_id, problem_slug, position) VALUES (?, ?, ?)",
                    problemList.id(),
                    slugs.get(i),
                    i
            );
        }
    }

    @Override
    public Optional<ProblemList> findList(String listId) {
        List<ProblemList> lists = jdbc.query(
                "SELECT id, name, source FROM problem_lists WHERE id = ?",
                (rs, rowNum) -> new ProblemList(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("source"),
                        new ArrayList<>()
                ),
                listId
        );
        if (lists.isEmpty()) {
            return Optional.empty();
        }
        ProblemList list = lists.getFirst();
        List<String> slugs = jdbc.query(
                "SELECT problem_slug FROM problem_list_items WHERE list_id = ? ORDER BY position",
                (rs, rowNum) -> rs.getString("problem_slug"),
                listId
        );
        return Optional.of(new ProblemList(list.id(), list.name(), list.source(), slugs));
    }

    @Override
    public List<ProblemList> findAllLists() {
        return jdbc.query("SELECT id, name, source FROM problem_lists ORDER BY id", (rs, rowNum) -> {
            String id = rs.getString("id");
            List<String> slugs = jdbc.query(
                    "SELECT problem_slug FROM problem_list_items WHERE list_id = ? ORDER BY position",
                    (innerRs, innerRowNum) -> innerRs.getString("problem_slug"),
                    id
            );
            return new ProblemList(id, rs.getString("name"), rs.getString("source"), slugs);
        });
    }

    @Override
    public void saveTopic(Topic topic) {
        jdbc.update(
                "INSERT OR REPLACE INTO topics(id, name, category) VALUES (?, ?, ?)",
                topic.id(),
                topic.name(),
                topic.category()
        );
    }

    @Override
    public List<Topic> findAllTopics() {
        return jdbc.query(
                "SELECT id, name, category FROM topics ORDER BY id",
                (rs, rowNum) -> new Topic(rs.getString("id"), rs.getString("name"), rs.getString("category"))
        );
    }

    @Override
    public void saveProblemTopic(String problemSlug, String topicId) {
        jdbc.update(
                "INSERT OR REPLACE INTO problem_topics(problem_slug, topic_id) VALUES (?, ?)",
                problemSlug,
                topicId
        );
    }

    @Override
    public List<String> findTopics(String problemSlug) {
        return jdbc.query(
                "SELECT topic_id FROM problem_topics WHERE problem_slug = ? ORDER BY topic_id",
                (rs, rowNum) -> rs.getString("topic_id"),
                problemSlug
        );
    }

    @Override
    public void saveMistake(MistakeNote note) {
        jdbc.update(
                "INSERT OR REPLACE INTO mistakes(problem_slug, error_type, stuck_point, lesson, similar_problems, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                note.problemSlug(),
                note.errorType(),
                note.stuckPoint(),
                note.lesson(),
                note.similarProblems(),
                Instant.now().toString()
        );
    }

    @Override
    public void deleteMistake(String problemSlug) {
        jdbc.update("DELETE FROM mistakes WHERE problem_slug = ?", problemSlug);
    }

    @Override
    public Optional<MistakeNote> findMistake(String problemSlug) {
        List<MistakeNote> notes = jdbc.query(
                "SELECT problem_slug, error_type, stuck_point, lesson, similar_problems FROM mistakes WHERE problem_slug = ?",
                (rs, rowNum) -> new MistakeNote(
                        rs.getString("problem_slug"),
                        rs.getString("error_type"),
                        rs.getString("stuck_point"),
                        rs.getString("lesson"),
                        rs.getString("similar_problems")
                ),
                problemSlug
        );
        return notes.stream().findFirst();
    }

    @Override
    public List<MistakeNote> findAllMistakes() {
        return jdbc.query(
                "SELECT problem_slug, error_type, stuck_point, lesson, similar_problems FROM mistakes ORDER BY created_at DESC",
                (rs, rowNum) -> new MistakeNote(
                        rs.getString("problem_slug"),
                        rs.getString("error_type"),
                        rs.getString("stuck_point"),
                        rs.getString("lesson"),
                        rs.getString("similar_problems")
                )
        );
    }

    @Override
    public void saveReview(Review review) {
        jdbc.update(
                "INSERT INTO reviews(problem_slug, reviewed_at, passed, notes) VALUES (?, ?, ?, ?)",
                review.problemSlug(),
                review.reviewedAt() == null ? null : review.reviewedAt().toString(),
                review.passed() ? 1 : 0,
                review.notes()
        );
    }

    @Override
    public List<Review> findReviews(String problemSlug) {
        return jdbc.query(
                "SELECT problem_slug, reviewed_at, passed, notes FROM reviews WHERE problem_slug = ? ORDER BY reviewed_at DESC",
                (rs, rowNum) -> new Review(
                        rs.getString("problem_slug"),
                        parseInstant(rs.getString("reviewed_at")),
                        rs.getInt("passed") == 1,
                        rs.getString("notes")
                ),
                problemSlug
        );
    }

    @Override
    public void savePlan(DailyPlan plan) {
        jdbc.update(
                "INSERT OR REPLACE INTO daily_plans(plan_date, core_tasks_json, bonus_tasks_json, completed) VALUES (?, ?, ?, ?)",
                plan.date().toString(),
                toJson(plan.coreTasks()),
                toJson(plan.bonusTasks()),
                plan.completed() ? 1 : 0
        );
    }

    @Override
    public Optional<DailyPlan> findPlan(LocalDate date) {
        List<DailyPlan> plans = jdbc.query(
                "SELECT plan_date, core_tasks_json, bonus_tasks_json, completed FROM daily_plans WHERE plan_date = ?",
                (rs, rowNum) -> new DailyPlan(
                        LocalDate.parse(rs.getString("plan_date")),
                        fromJson(rs.getString("core_tasks_json"), new TypeReference<List<PlanTask>>() {}),
                        fromJson(rs.getString("bonus_tasks_json"), new TypeReference<List<PlanTask>>() {}),
                        rs.getInt("completed") == 1
                ),
                date.toString()
        );
        return plans.stream().findFirst();
    }

    @Override
    public List<LocalDate> findCompletedPlanDates() {
        return jdbc.query(
                "SELECT plan_date FROM daily_plans WHERE completed = 1 ORDER BY plan_date DESC",
                (rs, rowNum) -> LocalDate.parse(rs.getString("plan_date"))
        );
    }

    @Override
    public void saveGoal(UserGoal goal) {
        jdbc.update(
                "INSERT OR REPLACE INTO user_goals(id, active_list_id, target_type, target) VALUES (1, ?, ?, ?)",
                goal.activeListId(),
                goal.targetType(),
                goal.target()
        );
    }

    @Override
    public Optional<UserGoal> findGoal() {
        List<UserGoal> goals = jdbc.query(
                "SELECT active_list_id, target_type, target FROM user_goals WHERE id = 1",
                (rs, rowNum) -> new UserGoal(
                        rs.getString("active_list_id"),
                        rs.getString("target_type"),
                        rs.getInt("target")
                )
        );
        return goals.stream().findFirst();
    }

    @Override
    public void saveInsight(String type, String content) {
        jdbc.update(
                "INSERT INTO insights(type, generated_at, content_json) VALUES (?, ?, ?)",
                type,
                Instant.now().toString(),
                content
        );
    }

    @Override
    public Optional<String> findLatestInsight(String type) {
        List<String> contents = jdbc.query(
                "SELECT content_json FROM insights WHERE type = ? ORDER BY generated_at DESC LIMIT 1",
                (rs, rowNum) -> rs.getString("content_json"),
                type
        );
        return contents.stream().findFirst();
    }

    @Override
    public void clearPracticeData() {
        jdbc.update("DELETE FROM submissions");
        jdbc.update("DELETE FROM problem_states");
        jdbc.update("DELETE FROM mistakes");
        jdbc.update("DELETE FROM reviews");
        jdbc.update("DELETE FROM daily_plans");
        jdbc.update("DELETE FROM problem_topics");
        jdbc.update("DELETE FROM problems");
        jdbc.update("DELETE FROM recommendations");
        jdbc.update("DELETE FROM stuck_events");
        jdbc.update("DELETE FROM notes");
        jdbc.update("DELETE FROM insights");
    }

    private RowMapper<Problem> problemRowMapper() {
        return (rs, rowNum) -> new Problem(
                rs.getString("slug"),
                rs.getString("title"),
                parseDifficulty(rs.getString("difficulty")),
                fromJson(rs.getString("tags_json"), new TypeReference<List<String>>() {}),
                fromJson(rs.getString("topics_json"), new TypeReference<List<String>>() {})
        );
    }

    private RowMapper<ProblemState> stateRowMapper() {
        return (rs, rowNum) -> new ProblemState(
                rs.getString("problem_slug"),
                rs.getInt("mastery_level"),
                rs.getInt("ac_count"),
                rs.getInt("attempt_count"),
                rs.getInt("is_mistake") == 1,
                rs.getString("mistake_type"),
                parseInstant(rs.getString("last_review_at")),
                parseInstant(rs.getString("next_review_at")),
                rs.getInt("review_count"),
                parseInstant(rs.getString("first_ac_at"))
        );
    }

    private Difficulty parseDifficulty(String value) {
        return value == null || value.isBlank() ? null : Difficulty.valueOf(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize JSON", e);
        }
    }

    private <T> T fromJson(String json, TypeReference<T> type) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse JSON", e);
        }
    }

    private String toInstantString(Instant instant) {
        return instant == null ? null : instant.toString();
    }

    private Instant parseInstant(String value) {
        return value == null ? null : Instant.parse(value);
    }
}
