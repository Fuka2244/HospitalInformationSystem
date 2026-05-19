package com.hospitalinfo.hospitalinformationsystem.ai;

import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 药品检索评测命令行工具
 * 
 * 使用方法：
 * 1. 在 application.yaml 中配置 ai.qwen 相关参数
 * 2. 确保数据库中有药品数据
 * 3. 运行应用时添加 --benchmark 参数来执行评测
 * 
 * 运行命令：
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--benchmark"
 * 
 * 跳过评测：
 *   mvn spring-boot:run -Dspring-boot.run.arguments="--skip-benchmark"
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicineSearchBenchmarkRunner implements CommandLineRunner {

    private final MedicineSearchEvaluator evaluator;
    private final MedicineMapper medicineMapper;

    // 预定义的高质量评测数据集（症状 -> 相关药品ID）
    // 注意：需要根据实际数据库中的药品ID进行调整
    private static final Map<String, Set<Long>> EVALUATION_DATASET = Map.of(
        "发烧头痛", Set.of(1L, 2L, 3L),
        "咳嗽感冒", Set.of(4L, 5L, 6L),
        "胃痛腹胀", Set.of(7L, 8L),
        "高血压", Set.of(9L, 10L),
        "糖尿病", Set.of(11L, 12L),
        "消炎抗菌", Set.of(13L, 14L, 15L),
        "退烧止痛", Set.of(1L, 16L, 17L),
        "过敏皮疹", Set.of(18L, 19L),
        "腹泻呕吐", Set.of(20L, 21L),
        "失眠焦虑", Set.of(22L, 23L)
    );

    @Override
    public void run(String... args) throws Exception {
        log.info("========== 药品检索系统 Benchmark 评测 ==========");
        
        // 检查是否有 --benchmark 参数
        boolean runBenchmark = false;
        for (String arg : args) {
            if ("--benchmark".equals(arg) || "--eval".equals(arg)) {
                runBenchmark = true;
                break;
            }
        }

        // 默认总是运行评测（可以在启动时加 --skip-benchmark 跳过）
        if (!containsArg(args, "--skip-benchmark")) {
            runEvaluation();
        }
    }

    private boolean containsArg(String[] args, String target) {
        for (String arg : args) {
            if (target.equals(arg)) return true;
        }
        return false;
    }

    private void runEvaluation() {
        try {
            // 1. 准备评测数据
            log.info("准备评测数据集...");
            prepareEvaluationData();

            // 2. 运行完整评测
            log.info("开始评测（请耐心等待）...");
            MedicineSearchEvaluator.ComparisonResult result = evaluator.runFullEvaluation();

            // 3. 导出并打印报告
            String report = evaluator.exportReport(result);
            log.info("\n{}", report);

            // 4. 保存报告到文件
            saveReportToFile(report);

        } catch (Exception e) {
            log.error("评测执行失败", e);
            log.info("提示：请确保已完成以下准备：");
            log.info("  1. 数据库中有药品数据");
            log.info("  2. 配置了有效的 AI API Key");
            log.info("  3. 向量索引已构建（medicine-embedding-store.json）");
        }
    }

    private void prepareEvaluationData() {
        // 添加预定义评测数据
        for (Map.Entry<String, Set<Long>> entry : EVALUATION_DATASET.entrySet()) {
            evaluator.addGroundTruth(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        // 尝试从数据库自动补充（基于药品名相似度生成伪标签）
        try {
            List<Medicine> medicines = medicineMapper.selectList(null);
            log.info("数据库中有 {} 种药品", medicines.size());

            // 自动生成部分评测用例
            int autoCount = 0;
            for (Medicine med : medicines) {
                if (autoCount >= 20) break;
                if (med.getName() != null && med.getName().length() >= 2) {
                    // 使用药品名作为查询，自身作为相关结果
                    Set<Long> relevant = new HashSet<>();
                    relevant.add(med.getId());
                    evaluator.addGroundTruth(med.getName(), relevant);
                    autoCount++;
                }
            }
            log.info("自动生成 {} 条评测用例", autoCount);

        } catch (Exception e) {
            log.warn("无法从数据库加载药品数据: {}", e.getMessage());
        }

        log.info("评测数据集准备完成，共 {} 条用例", evaluator.getDatasetSize());
    }

    private void saveReportToFile(String report) {
        try {
            String filename = "evaluation_report_" + 
                java.time.LocalDateTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
                ) + ".txt";

            java.nio.file.Path path = java.nio.file.Paths.get(filename);
            java.nio.file.Files.writeString(path, report);

            log.info("评测报告已保存至: {}", path.toAbsolutePath());
        } catch (Exception e) {
            log.warn("报告保存失败: {}", e.getMessage());
        }
    }

    /**
     * 便捷方法：运行轻量级评测（仅使用预定义数据集）
     */
    public void runLightBenchmark() {
        log.info("运行轻量级评测...");
        
        for (Map.Entry<String, Set<Long>> entry : EVALUATION_DATASET.entrySet()) {
            evaluator.addGroundTruth(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        try {
            MedicineSearchEvaluator.ComparisonResult result = evaluator.runFullEvaluation();
            System.out.println(evaluator.exportReport(result));
        } catch (Exception e) {
            log.error("轻量级评测失败", e);
        }
    }

    /**
     * 便捷方法：运行指定模式的评测
     */
    public void runSingleModeBenchmark(MedicineSearchEvaluator.RetrievalMode mode) {
        log.info("运行单模式评测: {}", mode.getDescription());
        
        for (Map.Entry<String, Set<Long>> entry : EVALUATION_DATASET.entrySet()) {
            evaluator.addGroundTruth(entry.getKey(), new HashSet<>(entry.getValue()));
        }

        try {
            MedicineSearchEvaluator.EvaluationResult result = evaluator.evaluateMode(mode);
            System.out.println("模式: " + mode.getDescription());
            System.out.println("P@10: " + result.getAverageMetrics().getPrecisionAt10());
            System.out.println("R@10: " + result.getAverageMetrics().getRecallAt10());
            System.out.println("F1@10: " + result.getAverageMetrics().getF1At10());
            System.out.println("NDCG@10: " + result.getAverageMetrics().getNdcgAt10());
            System.out.println("MRR: " + result.getAverageMetrics().getMrr());
            System.out.println("延迟: " + result.getAverageMetrics().getLatencyMs() + "ms");
        } catch (Exception e) {
            log.error("单模式评测失败", e);
        }
    }
}
