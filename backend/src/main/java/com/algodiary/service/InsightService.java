package com.algodiary.service;

import com.algodiary.model.Problem;
import com.algodiary.model.ProblemState;
import com.algodiary.model.Topic;
import com.algodiary.store.AlgoStore;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.algodiary.dto.TopicStats;
import com.algodiary.llm.LlmGateway;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InsightService {

    private static final String TYPE_SUMMARY = "summary";

    private final AlgoStore store;
    private final AnalyzerService analyzer;
    private final TopicService topicService;
    private final LlmGateway llm;

    public InsightService(AlgoStore store, AnalyzerService analyzer, TopicService topicService, LlmGateway llm) {
        this.store = store;
        this.analyzer = analyzer;
        this.topicService = topicService;
        this.llm = llm;
    }

    public String summary() {
        return store.findLatestInsight(TYPE_SUMMARY)
                .orElseGet(() -> {
                    String content = generateSummary();
                    store.saveInsight(TYPE_SUMMARY, content);
                    return content;
                });
    }

    public String refreshSummary() {
        String content = generateSummary();
        store.saveInsight(TYPE_SUMMARY, content);
        return content;
    }

    @Scheduled(cron = "0 0 8 * * MON")
    public void scheduledWeeklyRefresh() {
        refreshSummary();
    }

    private String generateSummary() {
        List<Problem> problems = store.findAllProblems();
        List<ProblemState> states = store.findAllStates();
        Set<String> topicIds = topicService.findAllTopics().stream()
                .map(Topic::id)
                .collect(Collectors.toSet());
        List<TopicStats> stats = analyzer.analyze(problems, states, List.of(), topicIds, 1);
        int solved = (int) states.stream().filter(state -> state.acCount() > 0).count();
        int mistakes = store.findAllMistakes().size();

        if (llm != null && llm.isConfigured()) {
            return llmSummary(stats, solved, mistakes);
        }
        return ruleBasedSummary(stats, solved, mistakes);
    }

    public String ruleBasedSummary(List<TopicStats> stats, int solved, int mistakes) {
        List<TopicStats> weak = stats.stream()
                .filter(TopicStats::weak)
                .sorted(Comparator.comparingDouble(TopicStats::acRate))
                .toList();
        List<TopicStats> strong = stats.stream()
                .filter(TopicStats::strong)
                .sorted(Comparator.comparingDouble(TopicStats::acRate).reversed())
                .toList();

        StringBuilder text = new StringBuilder();
        text.append("当前已攻克 ").append(solved).append(" 道题，错题 ").append(mistakes).append(" 道。");

        if (weak.isEmpty()) {
            text.append(" 暂无明显薄弱题型，继续按题单推进即可。");
        } else {
            text.append(" 薄弱题型：");
            for (TopicStats item : weak) {
                text.append(item.topicId())
                        .append("(AC ").append(Math.round(item.acRate() * 100)).append("%)、");
            }
            text.setLength(text.length() - 1);
        }

        if (!strong.isEmpty()) {
            text.append(" 保持较好的题型：");
            for (TopicStats item : strong) {
                text.append(item.topicId()).append("、");
            }
            text.setLength(text.length() - 1);
        }
        return text.toString();
    }

    private String llmSummary(List<TopicStats> stats, int solved, int mistakes) {
        String system = "你是算法学习教练，根据统计数据生成简短、鼓励性的周报，指出薄弱点并给出行动建议。";
        String user = "已攻克题数：" + solved + "\n错题数：" + mistakes + "\n题型统计：" + stats;
        return llm.complete(system, user);
    }
}
