package com.algodiary.service;

import com.algodiary.dto.MemoryProfile;
import com.algodiary.leetcode.LeetCodeClient;
import com.algodiary.leetcode.ProblemInfo;
import com.algodiary.llm.LlmGateway;
import com.algodiary.model.*;
import com.algodiary.store.AlgoStore;
import com.algodiary.support.InMemoryAlgoStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class TutorServiceTest {

    private InMemoryAlgoStore store;
    private FakeLlmGateway llm;
    private TutorService service;
    private AgentMemoryService memoryService;

    @BeforeEach
    void setUp() {
        store = new InMemoryAlgoStore();
        store.saveList(new ProblemList("hot-100", "LeetCode 热题 100", "BUILTIN", List.of("two-sum")));
        store.saveGoal(new UserGoal("hot-100", "COMPLETE_LIST", 100, 3));
        llm = new FakeLlmGateway(true, "模拟回答");
        TopicServiceStub topicService = new TopicServiceStub(store);
        memoryService = new AgentMemoryService(store, new AnalyzerService(), topicService,
                new ProblemListServiceStub(store), new GoalServiceStub(store), new JsonMapperStub());
        service = new TutorService(llm, memoryService, new AnalyzerService(), store,
                new LeetCodeClientStub(), topicService);
    }

    @Test
    void chatStoresUserAndAssistantMessages() {
        String sessionId = service.createSession("测试会话").id();

        service.chat(sessionId, "你好");

        List<TutorMessage> messages = store.findTutorMessages(sessionId, 20);
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).role()).isEqualTo("user");
        assertThat(messages.get(0).content()).isEqualTo("你好");
        assertThat(messages.get(1).role()).isEqualTo("assistant");
        assertThat(messages.get(1).content()).isEqualTo("模拟回答");
    }

    @Test
    void sessionsAreIsolated() {
        String sessionA = service.createSession("会话A").id();
        String sessionB = service.createSession("会话B").id();

        service.chat(sessionA, "只问A的问题");
        service.chat(sessionB, "只问B的问题");

        List<TutorMessage> historyA = store.findTutorMessages(sessionA, 20);
        List<TutorMessage> historyB = store.findTutorMessages(sessionB, 20);
        assertThat(historyA).hasSize(2);
        assertThat(historyB).hasSize(2);
        assertThat(historyA.get(0).content()).isEqualTo("只问A的问题");
        assertThat(historyB.get(0).content()).isEqualTo("只问B的问题");
    }

    @Test
    void deleteSessionRemovesMessages() {
        String sessionId = service.createSession("待删除").id();
        service.chat(sessionId, "测试");
        assertThat(store.findTutorMessages(sessionId, 20)).hasSize(2);

        service.deleteSession(sessionId);

        assertThat(store.findAllTutorSessions()).isEmpty();
        assertThat(store.findTutorMessages(sessionId, 20)).isEmpty();
    }

    @Test
    void clearSessionKeepsSessionButClearsMessages() {
        String sessionId = service.createSession("清空").id();
        service.chat(sessionId, "测试");

        service.clearSession(sessionId);

        assertThat(store.findAllTutorSessions()).hasSize(1);
        assertThat(store.findTutorMessages(sessionId, 20)).isEmpty();
    }

    @Test
    void rememberFactMergesIntoLongTermFacts() {
        service.rememberFact("用户目标是3个月刷完hot-100");

        List<String> facts = memoryService.getLongTermFacts();
        assertThat(facts).contains("用户目标是3个月刷完hot-100");
    }

    @Test
    void rememberFactDeduplicates() {
        service.rememberFact("用户目标是3个月刷完hot-100");
        service.rememberFact("用户目标是3个月刷完hot-100");

        assertThat(memoryService.getLongTermFacts()).hasSize(1);
    }

    @Test
    void ruleBasedReplyForWeakTopics() {
        FakeLlmGateway unconfigured = new FakeLlmGateway(false, "");
        TutorService ruleService = new TutorService(unconfigured, memoryService, new AnalyzerService(), store,
                new LeetCodeClientStub(), new TopicServiceStub(store));

        String reply = ruleService.chat("s1", "我的薄弱点是什么");

        assertThat(reply).contains("已攻克");
    }

    @Test
    void ruleBasedReplyWithoutConfig() {
        FakeLlmGateway unconfigured = new FakeLlmGateway(false, "");
        TutorService ruleService = new TutorService(unconfigured, memoryService, new AnalyzerService(), store,
                new LeetCodeClientStub(), new TopicServiceStub(store));

        String reply = ruleService.chat("s2", "随便聊聊");

        assertThat(reply).contains("尚未配置 AI 模型");
    }

    @Test
    void resolveProblemFallsBackToRemoteFetch() {
        // 本地无题，远程有题 → 应实时拉取并缓存
        Problem resolved = service.resolveProblem("two-sum");

        assertThat(resolved).isNotNull();
        assertThat(resolved.slug()).isEqualTo("two-sum");
        assertThat(resolved.title()).isEqualTo("Two Sum");
        // 已缓存
        assertThat(store.findProblem("two-sum")).isPresent();
    }

    @Test
    void resolveProblemReturnsNullWhenBothMiss() {
        Problem resolved = service.resolveProblem("not-exist-slug-xyz");
        assertThat(resolved).isNull();
    }

    @Test
    void chatStreamDeliversDeltas() {
        AtomicReference<String> received = new AtomicReference<>("");
        String sessionId = service.createSession("流式").id();

        service.chatStream(sessionId, "流式测试", delta -> received.set(received.get() + delta));

        assertThat(received.get()).isEqualTo("模拟回答");
    }

    // ==================== 测试替身 ====================

    private record FakeLlmGateway(boolean configured, String answer) implements LlmGateway {
        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public String complete(String system, String user) {
            return answer;
        }

        @Override
        public String chat(String system, List<ChatMessage> history) {
            return answer;
        }

        @Override
        public void chatStream(String system, List<ChatMessage> history, Consumer<String> onDelta) {
            onDelta.accept(answer);
        }
    }

    private static class LeetCodeClientStub extends LeetCodeClient {
        LeetCodeClientStub() {
            super(org.springframework.web.client.RestClient.builder(),
                    new com.algodiary.config.LeetCodeProperties(null, null, null, null),
                    new com.algodiary.config.LeetCodeCredentials(
                            new com.algodiary.config.LeetCodeProperties(null, null, null, null)));
        }

        @Override
        public Optional<ProblemInfo> fetchProblem(String titleSlug) {
            if ("two-sum".equals(titleSlug)) {
                return Optional.of(new ProblemInfo("two-sum", "Two Sum", "EASY", List.of("array", "hash-table")));
            }
            return Optional.empty();
        }
    }

    private static class TopicServiceStub extends TopicService {
        TopicServiceStub(AlgoStore store) {
            super(store);
        }
    }

    private static class ProblemListServiceStub extends ProblemListService {
        ProblemListServiceStub(InMemoryAlgoStore store) {
            super(store, new JsonMapperStub(), null, null);
        }
    }

    private static class GoalServiceStub extends GoalService {
        GoalServiceStub(AlgoStore store) {
            super(store, null);
        }
    }

    private static class JsonMapperStub extends com.fasterxml.jackson.databind.ObjectMapper {
    }
}
