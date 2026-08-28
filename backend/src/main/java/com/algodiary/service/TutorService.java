package com.algodiary.service;

import com.algodiary.dto.MemoryProfile;
import com.algodiary.dto.TopicStats;
import com.algodiary.leetcode.LeetCodeClient;
import com.algodiary.leetcode.ProblemInfo;
import com.algodiary.llm.LlmGateway;
import com.algodiary.model.*;
import com.algodiary.store.AlgoStore;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class TutorService {

    private static final int CONTEXT_WINDOW = 20;
    private static final int SUMMARIZE_EVERY = 10;
    private static final String SESSION_PREFIX = "tutor-";

    private final LlmGateway llm;
    private final AgentMemoryService memoryService;
    private final AnalyzerService analyzer;
    private final AlgoStore store;
    private final LeetCodeClient leetCodeClient;
    private final TopicService topicService;

    public TutorService(LlmGateway llm, AgentMemoryService memoryService, AnalyzerService analyzer,
                        AlgoStore store, LeetCodeClient leetCodeClient, TopicService topicService) {
        this.llm = llm;
        this.memoryService = memoryService;
        this.analyzer = analyzer;
        this.store = store;
        this.leetCodeClient = leetCodeClient;
        this.topicService = topicService;
    }

    // ==================== 会话管理 ====================

    public List<TutorSession> sessions() {
        return store.findAllTutorSessions();
    }

    public TutorSession createSession(String name) {
        String id = SESSION_PREFIX + UUID.randomUUID().toString().substring(0, 8);
        String normalized = name == null || name.isBlank() ? "新对话" : name.trim();
        store.saveTutorSession(id, normalized);
        return store.findAllTutorSessions().stream()
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElse(new TutorSession(id, normalized, java.time.Instant.now(), java.time.Instant.now()));
    }

    public void renameSession(String sessionId, String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("会话名称不能为空");
        }
        store.touchTutorSession(sessionId, name.trim());
    }

    public void deleteSession(String sessionId) {
        store.deleteTutorSession(sessionId);
    }

    public void clearSession(String sessionId) {
        store.clearTutorMessages(sessionId);
    }

    public List<TutorMessage> history(String sessionId, int limit) {
        return store.findTutorMessages(sessionId, Math.min(Math.max(limit, 1), 100));
    }

    // ==================== 对话 ====================

    public void chatStream(String sessionId, String message, Consumer<String> onDelta) {
        ensureSession(sessionId);
        store.saveTutorMessage(sessionId, "user", message);
        store.touchTutorSession(sessionId, autoName(sessionId, message));

        StringBuilder reply = new StringBuilder();
        if (llm.isConfigured()) {
            String system = buildSystemPrompt(sessionId);
            List<LlmGateway.ChatMessage> history = buildHistory(sessionId);
            llm.chatStream(system, history, delta -> {
                reply.append(delta);
                onDelta.accept(delta);
            });
        } else {
            String fallback = ruleBasedReply(message);
            reply.append(fallback);
            onDelta.accept(fallback);
        }
        store.saveTutorMessage(sessionId, "assistant", reply.toString());
        store.touchTutorSession(sessionId, null);

        maybeSummarize(sessionId);
    }

    public String chat(String sessionId, String message) {
        StringBuilder reply = new StringBuilder();
        chatStream(sessionId, message, reply::append);
        return reply.toString();
    }

    public TutorSession ensureSession(String sessionId) {
        boolean exists = store.findAllTutorSessions().stream().anyMatch(s -> s.id().equals(sessionId));
        if (!exists) {
            store.saveTutorSession(sessionId, "新对话");
        }
        return store.findAllTutorSessions().stream()
                .filter(s -> s.id().equals(sessionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
    }

    private String autoName(String sessionId, String message) {
        TutorSession current = store.findAllTutorSessions().stream()
                .filter(s -> s.id().equals(sessionId))
                .findFirst()
                .orElse(null);
        if (current == null || (!"新对话".equals(current.name()) && current.name() != null && !current.name().isBlank())) {
            return null;
        }
        String name = message == null ? "" : message.trim();
        return name.length() > 20 ? name.substring(0, 20) : name;
    }

    private List<LlmGateway.ChatMessage> buildHistory(String sessionId) {
        return store.findTutorMessages(sessionId, CONTEXT_WINDOW).stream()
                .map(m -> new LlmGateway.ChatMessage(m.role(), m.content()))
                .toList();
    }

    private String buildSystemPrompt(String sessionId) {
        MemoryProfile profile = memoryService.getProfile();
        List<String> longTermFacts = memoryService.getLongTermFacts();

        return """
                你是「算法伴学助手」的 AI 算法助教，用户是一位正在备战算法面试的开发者。
                这是用户当前的学习画像（JSON）：
                %s
                
                这是用户长期记忆中的重要事实（跨会话积累，可作为参考，但不要当作用户已确认的最新数据）：
                %s
                
                回答要求：
                1. 基于画像中的真实数据回答，不要编造用户的做题记录；
                2. 分析薄弱点时要给出具体数据依据（AC率、遗忘率、尝试次数）；
                3. 建议要可执行（具体到题、题型、方法），避免空话；
                4. 语气鼓励但不浮夸，像一位严谨的学长；
                5. 用中文回答，代码示例用 Markdown 代码块。
                """.formatted(toJson(profile), toJson(longTermFacts));
    }

    // ==================== 解析支持 ====================

    /** 本地查题，查不到时实时从 leetcode 拉取并缓存 */
    public Problem resolveProblem(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return store.findProblem(slug)
                .orElseGet(() -> {
                    try {
                        ProblemInfo info = leetCodeClient.fetchProblem(slug).orElse(null);
                        if (info == null || info.titleSlug() == null) {
                            return null;
                        }
                        Difficulty difficulty = parseDifficulty(info.difficulty());
                        Problem problem = new Problem(info.titleSlug(), info.title(), difficulty, info.tags(), List.of());
                        store.saveProblem(problem);
                        return problem;
                    } catch (Exception e) {
                        return null;
                    }
                });
    }

    private Difficulty parseDifficulty(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Difficulty.valueOf(value.trim().toUpperCase());
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 记忆沉淀 ====================

    public List<String> rememberFact(String fact) {
        if (fact == null || fact.isBlank()) {
            throw new IllegalArgumentException("记忆内容不能为空");
        }
        return memoryService.mergeLongTermFacts(List.of(fact));
    }

    /** 自动提炼会话中的重要信息并沉淀（每 SUMMARIZE_EVERY 条或手动触发） */
    public List<String> summarizeSession(String sessionId) {
        List<TutorMessage> messages = store.findTutorMessages(sessionId, CONTEXT_WINDOW);
        if (messages.isEmpty() || !llm.isConfigured()) {
            return List.of();
        }
        String transcript = messages.stream()
                .map(m -> (m.role().equals("user") ? "用户: " : "助教: ") + m.content())
                .collect(Collectors.joining("\n"));
        if (transcript.length() > 6000) {
            transcript = transcript.substring(transcript.length() - 6000);
        }

        String system = "你是学习助手的记忆提炼器。从对话中提取值得长期记住的用户信息，"
                + "例如：用户的目标、反复出现的薄弱模式、学习偏好、重要的个人情况。"
                + "只输出 JSON 数组，如 [\"事实1\", \"事实2\"]，不要输出其他内容。";
        String user = "对话记录：\n" + transcript;
        String raw = llm.complete(system, user);
        List<String> facts = parseFacts(raw);
        return facts.isEmpty() ? List.of() : memoryService.mergeLongTermFacts(facts);
    }

    private List<String> parseFacts(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        int start = trimmed.indexOf('[');
        int end = trimmed.lastIndexOf(']');
        if (start >= 0 && end > start) {
            trimmed = trimmed.substring(start, end + 1);
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(trimmed, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private void maybeSummarize(String sessionId) {
        int userCount = (int) store.findTutorMessages(sessionId, 200).stream()
                .filter(m -> m.role().equals("user"))
                .count();
        if (userCount > 0 && userCount % SUMMARIZE_EVERY == 0) {
            try {
                summarizeSession(sessionId);
            } catch (Exception ignored) {
                // 提炼失败不影响主流程
            }
        }
    }

    // ==================== 规则降级 ====================

    public String ruleBasedReply(String message) {
        String text = message == null ? "" : message;
        if (text.contains("薄弱") || text.contains("弱项") || text.contains("分析")) {
            return ruleWeakTopics();
        }
        if (text.contains("错题") || text.contains("复盘")) {
            return ruleMistakes();
        }
        if (text.contains("今天") || text.contains("计划") || text.contains("刷什么") || text.contains("推荐")) {
            return rulePlan();
        }
        if (text.contains("解析") || text.startsWith("explain")) {
            return "解析功能需要配置 AI 模型。请先在右上角『AI 配置』填入 API Key。";
        }
        return "尚未配置 AI 模型，无法自由对话。建议在右上角『AI 配置』填入 API Key。\n"
                + "当前数据快照：\n" + ruleWeakTopics();
    }

    private String ruleWeakTopics() {
        MemoryProfile profile = memoryService.getProfile();
        StringBuilder sb = new StringBuilder();
        sb.append("已攻克 ").append(profile.solvedCount()).append(" 题，错题 ")
                .append(profile.mistakeCount()).append(" 道。\n");
        if (profile.weakTopics().isEmpty()) {
            sb.append("暂无明显薄弱题型，继续按题单推进即可。");
        } else {
            sb.append("薄弱题型：");
            profile.weakTopics().forEach(t -> sb.append(t).append("、"));
            sb.setLength(sb.length() - 1);
            sb.append("。建议优先补强这些题型的经典题。");
        }
        return sb.toString();
    }

    private String ruleMistakes() {
        List<MistakeNote> mistakes = store.findAllMistakes();
        if (mistakes.isEmpty()) {
            return "暂无错题记录，继续保持。";
        }
        StringBuilder sb = new StringBuilder("最近错题：\n");
        mistakes.stream().limit(5).forEach(m -> sb.append("- ").append(m.problemSlug())
                .append(m.errorType() == null ? "" : "（" + m.errorType() + "）")
                .append("\n"));
        sb.append("复盘建议：每题想清楚「卡在哪一步、下次怎么做」，并归类错误类型，找共性。");
        return sb.toString();
    }

    private String rulePlan() {
        MemoryProfile profile = memoryService.getProfile();
        if (profile.weakTopics().isEmpty()) {
            return "暂无薄弱题型数据，按当前题单继续推进即可。每日目标 " + profile.dailyTarget() + " 题。";
        }
        return "建议优先练习薄弱题型：" + String.join("、", profile.weakTopics())
                + "。结合每日 " + profile.dailyTarget() + " 题的目标，先各做 1-2 道经典题找感觉。";
    }

    private String toJson(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception e) {
            return "{}";
        }
    }

    // ==================== 画像快照（供 UI 展示可选项） ====================

    public Map<String, Object> profileSnapshot() {
        MemoryProfile profile = memoryService.getProfile();
        return Map.of(
                "solvedCount", profile.solvedCount(),
                "mistakeCount", profile.mistakeCount(),
                "dailyTarget", profile.dailyTarget(),
                "weakTopics", profile.weakTopics(),
                "longTermFacts", profile.longTermFacts()
        );
    }
}
