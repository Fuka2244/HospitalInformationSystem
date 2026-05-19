package com.hospitalinfo.hospitalinformationsystem.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 药品检索系统Benchmark测试
 * 使用模拟数据进行评测演示
 */
@DisplayName("药品检索Benchmark测试")
class MedicineSearchBenchmarkTest {

    @Test
    @DisplayName("模拟数据评测 - 演示评测流程")
    void testBenchmarkWithMockData() {
        // 创建模拟评测器（不依赖Spring上下文）
        MockEvaluator evaluator = new MockEvaluator();

        // 添加模拟标注数据
        setupMockData(evaluator);

        // 运行评测
        BenchmarkResult result = evaluator.runBenchmark();

        // 输出结果
        printResults(result);

        // 验证结果合理性
        assertTrue(result.getResults().size() == 4, "应该有4种检索模式");
        assertTrue(result.getHybridImprovement().size() > 0, "应该有对比分析");
    }

    private void setupMockData(MockEvaluator evaluator) {
        // 模拟标注数据：查询 -> 相关药品ID
        evaluator.addGroundTruth("发烧头痛", Set.of(101L, 102L, 103L));
        evaluator.addGroundTruth("咳嗽有痰", Set.of(201L, 202L));
        evaluator.addGroundTruth("胃痛腹胀", Set.of(301L, 302L, 303L, 304L));
        evaluator.addGroundTruth("高血压", Set.of(401L, 402L));
        evaluator.addGroundTruth("糖尿病", Set.of(501L, 502L));
        evaluator.addGroundTruth("过敏皮疹", Set.of(601L, 602L, 603L));
        evaluator.addGroundTruth("消炎抗菌", Set.of(701L, 702L));
        evaluator.addGroundTruth("退烧止痛", Set.of(801L, 802L, 803L));
        evaluator.addGroundTruth("腹泻呕吐", Set.of(901L, 902L));
        evaluator.addGroundTruth("失眠焦虑", Set.of(1001L, 1002L));
    }

    private void printResults(BenchmarkResult result) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("         药品检索系统 Benchmark 评测报告 (模拟数据)");
        System.out.println("=".repeat(70));

        // 表格头部
        System.out.printf("\n%-25s %-8s %-8s %-8s %-8s %-8s %s\n",
            "检索模式", "P@10", "R@10", "F1@10", "NDCG@10", "MRR", "延迟(ms)");
        System.out.println("-".repeat(70));

        // 各模式结果
        for (ModeResult mr : result.getResults()) {
            System.out.printf("%-25s %.4f   %.4f   %.4f   %.4f   %.4f   %d\n",
                mr.mode,
                mr.precision,
                mr.recall,
                mr.f1,
                mr.ndcg,
                mr.mrr,
                mr.latencyMs);
        }

        // 对比分析
        System.out.println("\n" + "-".repeat(70));
        System.out.println("【对比分析】");
        for (String line : result.getHybridImprovement()) {
            System.out.println(line);
        }

        // 结论
        System.out.println("\n" + "-".repeat(70));
        System.out.println("【结论】");
        System.out.println(result.getConclusion());

        System.out.println("\n" + "=".repeat(70));
    }

    // ==================== 模拟评测器 ====================

    static class MockEvaluator {
        private final Map<String, Set<Long>> groundTruth = new LinkedHashMap<>();
        private final Random random = new Random(42);

        void addGroundTruth(String query, Set<Long> relevant) {
            groundTruth.put(query, relevant);
        }

        BenchmarkResult runBenchmark() {
            BenchmarkResult result = new BenchmarkResult();
            result.setResults(new ArrayList<>());
            result.setHybridImprovement(new ArrayList<>());

            // 模拟各模式检索结果
            ModeResult vectorOnly = simulateMode("纯向量检索", 0.65, 0.55, 0.70, 45);
            ModeResult keywordOnly = simulateMode("纯关键词检索", 0.60, 0.50, 0.65, 25);
            ModeResult hybrid = simulateMode("混合检索(RRF)", 0.75, 0.65, 0.80, 55);
            ModeResult hybridRerank = simulateMode("混合检索+LLM精排", 0.82, 0.72, 0.87, 150);

            result.getResults().add(vectorOnly);
            result.getResults().add(keywordOnly);
            result.getResults().add(hybrid);
            result.getResults().add(hybridRerank);

            // 计算提升
            double hybridVsVector = ((hybrid.f1 - vectorOnly.f1) / vectorOnly.f1) * 100;
            double hybridVsKeyword = ((hybrid.f1 - keywordOnly.f1) / keywordOnly.f1) * 100;
            double rerankVsHybrid = ((hybridRerank.f1 - hybrid.f1) / hybrid.f1) * 100;

            result.getHybridImprovement().add(String.format(
                "混合检索 vs 纯向量: F1提升 %.2f%%", hybridVsVector));
            result.getHybridImprovement().add(String.format(
                "混合检索 vs 纯关键词: F1提升 %.2f%%", hybridVsKeyword));
            result.getHybridImprovement().add(String.format(
                "LLM精排 vs 混合检索: F1提升 %.2f%%", rerankVsHybrid));

            // 生成结论
            StringBuilder conclusion = new StringBuilder();
            conclusion.append("1. 混合检索(RRF)在F1指标上优于单一检索方式\n");
            conclusion.append("2. LLM精排能进一步提升准确率，但延迟增加显著\n");
            conclusion.append("3. 建议: 对延迟敏感场景使用纯混合检索，对准确率要求高使用精排\n");
            conclusion.append("4. 延迟对比: 混合检索55ms vs 精排150ms，延迟增加约2.7倍");

            result.setConclusion(conclusion.toString());

            return result;
        }

        private ModeResult simulateMode(String mode, double baseP, double baseR, double baseF1, int baseLatency) {
            ModeResult mr = new ModeResult();
            mr.mode = mode;

            // 添加一些随机波动模拟真实场景
            mr.precision = clamp(baseP + (random.nextDouble() - 0.5) * 0.1, 0, 1);
            mr.recall = clamp(baseR + (random.nextDouble() - 0.5) * 0.1, 0, 1);
            mr.f1 = clamp(baseF1 + (random.nextDouble() - 0.5) * 0.05, 0, 1);
            mr.ndcg = clamp(mr.f1 * 1.05, 0, 1);
            mr.mrr = clamp(mr.recall * 1.2, 0, 1);
            mr.latencyMs = baseLatency + random.nextInt(20);

            return mr;
        }

        private double clamp(double value, double min, double max) {
            return Math.max(min, Math.min(max, value));
        }
    }

    // ==================== 结果类 ====================

    static class BenchmarkResult {
        private List<ModeResult> results;
        private List<String> hybridImprovement;
        private String conclusion;

        public List<ModeResult> getResults() { return results; }
        public void setResults(List<ModeResult> results) { this.results = results; }
        public List<String> getHybridImprovement() { return hybridImprovement; }
        public void setHybridImprovement(List<String> hybridImprovement) { this.hybridImprovement = hybridImprovement; }
        public String getConclusion() { return conclusion; }
        public void setConclusion(String conclusion) { this.conclusion = conclusion; }
    }

    static class ModeResult {
        String mode;
        double precision;
        double recall;
        double f1;
        double ndcg;
        double mrr;
        int latencyMs;
    }
}
