package com.algodiary.service;

import com.algodiary.model.Problem;
import org.springframework.stereotype.Service;
import com.algodiary.llm.LlmGateway;

@Service
public class ExplainService {

    private final LlmGateway llm;

    public ExplainService(LlmGateway llm) {
        this.llm = llm;
    }

    public String explain(Problem problem, int hintLevel) {
        if (problem == null) {
            return "题目不存在";
        }
        if (llm != null && llm.isConfigured()) {
            return llmExplain(problem, hintLevel);
        }
        return ruleBased(problem, hintLevel);
    }

    private String llmExplain(Problem problem, int hintLevel) {
        String levelLabel = switch (hintLevel) {
            case 0 -> "方向提示（不要给答案）";
            case 1 -> "关键转换或边界提示（不要给完整代码）";
            default -> "完整思路解析";
        };
        String system = "你是算法教练，目标是引导用户自己做出 LeetCode 题目，"
                + "解释要简洁、逐步、避免直接给出完整代码。";
        String user = "题目：" + (problem.title() == null ? problem.slug() : problem.title())
                + "（slug: " + problem.slug() + "）\n难度：" + problem.difficulty()
                + "\n请给出：" + levelLabel;
        return llm.complete(system, user);
    }

    private String ruleBased(Problem problem, int hintLevel) {
        String title = problem.title() == null || problem.title().isBlank()
                ? problem.slug()
                : problem.title();
        return switch (hintLevel) {
            case 0 -> "题目：" + title + "。先明确输入输出和边界条件，尝试给出一个暴力解，再思考能否优化。";
            case 1 -> "题目：" + title + "。识别它属于哪类算法模式（双指针 / 滑动窗口 / 二分 / DP 等），先定义状态或指针移动规则。";
            case 2 -> "题目：" + title + "。如果仍卡住，建议先看 LeetCode 官方题解的前半段思路，然后自己写完代码并提交，最后回来复盘卡点。";
            default -> "题目：" + title + "。https://leetcode.cn/problems/" + problem.slug() + "/";
        };
    }
}
