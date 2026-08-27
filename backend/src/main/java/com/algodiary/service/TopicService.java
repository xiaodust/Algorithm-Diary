package com.algodiary.service;

import com.algodiary.model.Problem;
import com.algodiary.model.Topic;
import com.algodiary.store.AlgoStore;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class TopicService {

    private final AlgoStore store;

    public TopicService(AlgoStore store) {
        this.store = store;
    }

    @PostConstruct
    public void seedTopics() {
        KEYWORD_RULES.keySet().forEach(topicId -> {
            Topic topic = new Topic(topicId, displayName(topicId), category(topicId));
            store.saveTopic(topic);
        });
    }

    public List<Topic> findAllTopics() {
        return store.findAllTopics();
    }

    public List<String> classify(String title, List<String> tags) {
        Set<String> result = new LinkedHashSet<>();
        if (tags != null) {
            result.addAll(tags);
        }
        String normalized = title == null ? "" : title.toLowerCase();
        for (Map.Entry<String, List<String>> entry : KEYWORD_RULES.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (normalized.contains(keyword)) {
                    result.add(entry.getKey());
                    break;
                }
            }
        }
        return List.copyOf(result);
    }

    public Problem enrich(Problem problem) {
        List<String> topics = classify(problem.title(), problem.tags());
        Problem enriched = new Problem(problem.slug(), problem.title(), problem.difficulty(), problem.tags(), topics);
        store.saveProblem(enriched);
        return enriched;
    }

    public int enrichAll() {
        List<Problem> problems = store.findAllProblems();
        for (Problem problem : problems) {
            enrich(problem);
        }
        return problems.size();
    }

    private String displayName(String topicId) {
        return switch (topicId) {
            case "two-pointers" -> "双指针";
            case "sliding-window" -> "滑动窗口";
            case "binary-search" -> "二分查找";
            case "linked-list" -> "链表";
            case "stack" -> "栈";
            case "queue" -> "队列";
            case "heap-priority-queue" -> "堆/优先队列";
            case "tree" -> "树";
            case "graph" -> "图";
            case "depth-first-search" -> "DFS";
            case "breadth-first-search" -> "BFS";
            case "backtracking" -> "回溯";
            case "dynamic-programming" -> "动态规划";
            case "greedy" -> "贪心";
            case "sorting" -> "排序";
            case "trie" -> "Trie";
            case "union-find" -> "并查集";
            case "prefix-sum" -> "前缀和";
            case "monotonic-stack" -> "单调栈";
            case "hash-table" -> "哈希表";
            case "array" -> "数组";
            case "string" -> "字符串";
            default -> topicId;
        };
    }

    private String category(String topicId) {
        return Set.of("array", "string", "hash-table", "linked-list", "stack", "queue",
                "heap-priority-queue", "tree", "graph", "trie", "union-find", "monotonic-stack")
                .contains(topicId) ? "DATA_STRUCTURE" : "ALGORITHM";
    }

    private static final Map<String, List<String>> KEYWORD_RULES = Map.ofEntries(
            Map.entry("two-pointers", List.of("two-sum", "3sum", "4sum", "move-zero", "palindrome", "remove-duplicates", "container-with-most-water", "trapping-rain", "intersection-of-two", "linked-list-cycle", "remove-nth-node", "sort-colors")),
            Map.entry("sliding-window", List.of("sliding-window", "longest-substring", "minimum-window-substring", "maximum-length-of-repeated-subarray")),
            Map.entry("binary-search", List.of("binary-search", "search-in-rotated", "find-minimum-in-rotated", "find-peak-element", "median-of-two-sorted-arrays", "search-a-2d-matrix", "sqrtx")),
            Map.entry("linked-list", List.of("linked-list", "reverse-linked", "merge-k-sorted-lists", "add-two-numbers", "remove-nth-node", "reorder-list", "swap-nodes", "intersection-of-two-linked")),
            Map.entry("stack", List.of("min-stack", "valid-parentheses", "basic-calculator", "implement-queue-using-stacks", "decode-string", "longest-valid-parentheses", "trapping-rain-water", "monotonic-stack")),
            Map.entry("queue", List.of("implement-stack-using-queues", "sliding-window-maximum", "implement-queue")),
            Map.entry("heap-priority-queue", List.of("kth-largest", "top-k", "merge-k-sorted-lists", "median-of-two", "heap")),
            Map.entry("tree", List.of("binary-tree", "inorder-traversal", "preorder-traversal", "postorder-traversal", "level-order", "zigzag", "lowest-common-ancestor", "maximum-depth", "path-sum", "symmetric-tree", "invert-binary-tree", "validate-binary-search", "balanced-binary", "diameter", "sum-root-to-leaf", "maximum-width-of-binary-tree", "construct-binary-tree")),
            Map.entry("graph", List.of("number-of-islands", "max-area-of-island", "graph", "course-schedule", "shortest-path")),
            Map.entry("depth-first-search", List.of("number-of-islands", "max-area-of-island", "depth-first", "sum-root-to-leaf")),
            Map.entry("breadth-first-search", List.of("level-order", "zigzag", "breadth-first", "minimum-depth")),
            Map.entry("backtracking", List.of("subsets", "permutations", "combination-sum", "generate-parentheses", "restore-ip-addresses", "n-queens")),
            Map.entry("dynamic-programming", List.of("coin-change", "climbing-stairs", "maximum-subarray", "longest-increasing-subsequence", "longest-common-subsequence", "minimum-path-sum", "edit-distance", "unique-paths", "maximum-product-subarray", "house-robber", "maximal-square", "best-time-to-buy-and-sell", "dynamic-programming")),
            Map.entry("greedy", List.of("greedy", "jump-game", "best-time-to-buy-and-sell", "largest-number")),
            Map.entry("sorting", List.of("sort-an-array", "merge-intervals", "largest-number", "kth-largest")),
            Map.entry("trie", List.of("trie", "implement-trie")),
            Map.entry("union-find", List.of("union-find", "number-of-islands", "redundant-connection")),
            Map.entry("prefix-sum", List.of("prefix-sum", "subarray-sum", "range-sum")),
            Map.entry("monotonic-stack", List.of("monotonic-stack", "trapping-rain-water", "daily-temperatures", "next-greater")),
            Map.entry("hash-table", List.of("hash-table", "two-sum", "group-anagrams", "longest-consecutive", "single-number", "lru-cache")),
            Map.entry("array", List.of("array", "matrix", "rotate-image", "spiral-matrix", "merge-sorted-array", "majority-element")),
            Map.entry("string", List.of("string", "valid-parentheses", "longest-palindromic", "reverse-words", "multiply-strings", "longest-common-prefix"))
    );
}
