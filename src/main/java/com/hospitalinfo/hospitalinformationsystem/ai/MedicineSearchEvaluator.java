package com.hospitalinfo.hospitalinformationsystem.ai;

import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 药品检索系统评测框架
 * 支持三种检索模式对比：纯向量检索、纯关键词检索、混合检索
 * 评测指标：Precision@K, Recall@K, F1@K, NDCG@K, MRR
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicineSearchEvaluator {

    private final MedicineMapper medicineMapper;
    private final EmbeddingModel embeddingModel;
    // EmbeddingStore 需要时可注入，此处使用懒加载
    private volatile dev.langchain4j.store.embedding.EmbeddingStore<TextSegment> embeddingStore;

    // ==================== 评测数据集 ====================

    /**
     * 评测查询集
     * 格式：query -> 相关药品ID集合
     */
    private static final Map<String, Set<Long>> EVAL_QUERIES = new LinkedHashMap<>();

    static {
        // 症状 -> 相关药品 (需要根据实际数据填充)
        EVAL_QUERIES.put("发烧头痛", Set.of());
        EVAL_QUERIES.put("咳嗽感冒", Set.of());
        EVAL_QUERIES.put("胃痛消化不良", Set.of());
        EVAL_QUERIES.put("高血压", Set.of());
        EVAL_QUERIES.put("糖尿病", Set.of());
        EVAL_QUERIES.put("消炎抗菌", Set.of());
        EVAL_QUERIES.put("退烧止痛", Set.of());
        EVAL_QUERIES.put("过敏皮疹", Set.of());
        EVAL_QUERIES.put("腹泻呕吐", Set.of());
        EVAL_QUERIES.put("失眠焦虑", Set.of());
    }

    // ==================== 评测模式枚举 ====================

    public enum RetrievalMode {
        VECTOR_ONLY("纯向量检索"),
        KEYWORD_ONLY("纯关键词检索"),
        HYBRID("混合检索(混合+RRF)"),
        HYBRID_WITH_RERANK("混合检索+LLM精排");

        private final String description;

        RetrievalMode(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ==================== 评测指标类 ====================

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class RetrievalMetrics {
        private double precisionAt10;
        private double recallAt10;
        private double f1At10;
        private double ndcgAt10;
        private double mrr;              // Mean Reciprocal Rank
        private double averagePrecision; // MAP (Mean Average Precision)
        private long latencyMs;          // 检索延迟
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class EvaluationResult {
        private RetrievalMode mode;
        private List<RetrievalMetrics> perQueryMetrics;
        private RetrievalMetrics averageMetrics;
        private Map<String, Object> additionalInfo;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ComparisonResult {
        private List<EvaluationResult> results;
        private Map<String, Object> analysis;
        private String recommendation;
    }

    // ==================== 主评测方法 ====================

    /**
     * 运行完整评测流程
     */
    public ComparisonResult runFullEvaluation() {
        log.info("========== 开始药品检索系统评测 ==========");
        long startTime = System.currentTimeMillis();

        List<EvaluationResult> results = new ArrayList<>();

        // 1. 纯向量检索评测
        log.info("评测模式1: 纯向量检索");
        EvaluationResult vectorResult = evaluateMode(RetrievalMode.VECTOR_ONLY);
        results.add(vectorResult);

        // 2. 纯关键词检索评测
        log.info("评测模式2: 纯关键词检索");
        EvaluationResult keywordResult = evaluateMode(RetrievalMode.KEYWORD_ONLY);
        results.add(keywordResult);

        // 3. 混合检索评测
        log.info("评测模式3: 混合检索(混合+RRF)");
        EvaluationResult hybridResult = evaluateMode(RetrievalMode.HYBRID);
        results.add(hybridResult);

        // 4. 混合检索+LLM精排
        log.info("评测模式4: 混合检索+LLM精排");
        EvaluationResult rerankResult = evaluateMode(RetrievalMode.HYBRID_WITH_RERANK);
        results.add(rerankResult);

        // 分析对比结果
        ComparisonResult comparison = analyzeResults(results);

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("========== 评测完成，耗时: {}ms ==========", totalTime);

        return comparison;
    }

    /**
     * 评测指定模式
     */
    public EvaluationResult evaluateMode(RetrievalMode mode) {
        List<RetrievalMetrics> perQueryMetrics = new ArrayList<>();
        Map<String, Object> additionalInfo = new HashMap<>();

        long totalLatency = 0;
        double totalPrecision = 0;
        double totalRecall = 0;
        double totalF1 = 0;
        double totalNdcg = 0;
        double totalMrr = 0;
        double totalMap = 0;

        int queryCount = 0;

        for (Map.Entry<String, Set<Long>> entry : EVAL_QUERIES.entrySet()) {
            String query = entry.getKey();
            Set<Long> relevantIds = entry.getValue();

            if (relevantIds.isEmpty()) {
                log.debug("跳过无标注数据的查询: {}", query);
                continue;
            }

            long queryStart = System.currentTimeMillis();
            List<Long> retrievedIds = retrieve(mode, query, 10);
            long queryLatency = System.currentTimeMillis() - queryStart;

            // 计算各项指标
            RetrievalMetrics metrics = calculateMetrics(retrievedIds, relevantIds, 10, queryLatency);
            perQueryMetrics.add(metrics);

            totalLatency += queryLatency;
            totalPrecision += metrics.getPrecisionAt10();
            totalRecall += metrics.getRecallAt10();
            totalF1 += metrics.getF1At10();
            totalNdcg += metrics.getNdcgAt10();
            totalMrr += metrics.getMrr();
            totalMap += metrics.getAveragePrecision();

            queryCount++;
        }

        // 计算平均值
        if (queryCount > 0) {
            RetrievalMetrics avgMetrics = new RetrievalMetrics(
                totalPrecision / queryCount,
                totalRecall / queryCount,
                totalF1 / queryCount,
                totalNdcg / queryCount,
                totalMrr / queryCount,
                totalMap / queryCount,
                totalLatency / queryCount
            );

            log.info("【{}】 P@10={:.4f} R@10={:.4f} F1@10={:.4f} NDCG@10={:.4f} MRR={:.4f} 延迟={}ms",
                mode.getDescription(),
                avgMetrics.getPrecisionAt10(),
                avgMetrics.getRecallAt10(),
                avgMetrics.getF1At10(),
                avgMetrics.getNdcgAt10(),
                avgMetrics.getMrr(),
                avgMetrics.getLatencyMs());

            return new EvaluationResult(mode, perQueryMetrics, avgMetrics, additionalInfo);
        }

        return new EvaluationResult(mode, perQueryMetrics, 
            new RetrievalMetrics(0, 0, 0, 0, 0, 0, 0), additionalInfo);
    }

    /**
     * 根据模式执行检索
     */
    private List<Long> retrieve(RetrievalMode mode, String query, int topK) {
        switch (mode) {
            case VECTOR_ONLY:
                return vectorOnlySearch(query, topK);
            case KEYWORD_ONLY:
                return keywordOnlySearch(query, topK);
            case HYBRID:
                return hybridSearch(query, topK);
            case HYBRID_WITH_RERANK:
                return hybridSearchWithRerank(query, topK);
            default:
                return new ArrayList<>();
        }
    }

    // ==================== 检索实现 ====================

    /**
     * 纯向量检索
     */
    private List<Long> vectorOnlySearch(String query, int topK) {
        Map<Long, Double> scores = getVectorScores(query);
        return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(topK)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 纯关键词检索
     */
    private List<Long> keywordOnlySearch(String query, int topK) {
        Map<Long, Double> scores = getKeywordScores(query);
        return scores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(topK)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 混合检索 (向量 + 关键词 + RRF)
     */
    private List<Long> hybridSearch(String query, int topK) {
        // 获取两种检索的得分
        Map<Long, Double> vectorScores = getVectorScores(query);
        Map<Long, Double> keywordScores = getKeywordScores(query);

        // RRF融合
        return rrfFusion(vectorScores, keywordScores, topK);
    }

    /**
     * 混合检索 + LLM精排
     */
    private List<Long> hybridSearchWithRerank(String query, int topK) {
        // 第一阶段：混合检索获取候选集
        List<Long> candidates = hybridSearch(query, topK * 3);

        if (candidates.isEmpty()) {
            return candidates;
        }

        // 第二阶段：获取候选药品详情用于LLM精排
        List<String> candidateNames = candidates.stream()
            .map(id -> medicineMapper.selectById(id))
            .filter(m -> m != null)
            .map(m -> m.getName())
            .toList();

        // 第三阶段：调用LLM精排（这里简化处理，实际可调用专门的Rerank模型）
        // 由于LLM精排成本高，可以采用轻量级策略：
        // - 使用交叉编码器重排
        // - 或使用更小的蒸馏模型
        // 此处模拟精排效果
        return candidates.subList(0, Math.min(topK, candidates.size()));
    }

    // ==================== 得分计算 ====================

    private Map<Long, Double> getVectorScores(String query) {
        Map<Long, Double> scores = new HashMap<>();
        try {
            if (embeddingStore == null) {
                log.warn("EmbeddingStore 未注入，跳过向量检索");
                return scores;
            }
            
            Embedding queryEmbedding = embeddingModel.embed(query).content();
            
            EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(30)
                .minScore(0.3)
                .build();

            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            
            for (EmbeddingMatch<TextSegment> match : searchResult.matches()) {
                TextSegment segment = match.embedded();
                String medicineIdStr = segment.metadata().getString("medicineId");
                if (medicineIdStr != null) {
                    long medicineId = Long.parseLong(medicineIdStr);
                    scores.merge(medicineId, match.score(), Math::max);
                }
            }
        } catch (Exception e) {
            log.error("向量得分获取失败: {}", e.getMessage());
        }
        return scores;
    }

    private Map<Long, Double> getKeywordScores(String query) {
        Map<Long, Double> scores = new HashMap<>();
        String[] keywords = query.split("[，,。.、！!？?\\s]+");

        for (String keyword : keywords) {
            if (keyword.length() < 2) continue;

            List<Medicine> medicines = medicineMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Medicine>()
                    .and(w -> w
                        .like("name", keyword)
                        .or().like("generic_name", keyword)
                        .or().like("efficacy", keyword)
                        .or().like("ingredients", keyword)
                    )
            );

            for (Medicine med : medicines) {
                double score = calculateKeywordScore(med, keyword);
                scores.merge(med.getId(), score, Double::sum);
            }
        }
        return scores;
    }

    /**
     * 计算关键词命中得分
     */
    private double calculateKeywordScore(Medicine med, String keyword) {
        double score = 0;
        if (med.getName() != null && med.getName().contains(keyword)) {
            score += 3.0;
        }
        if (med.getGenericName() != null && med.getGenericName().contains(keyword)) {
            score += 2.5;
        }
        if (med.getEfficacy() != null && med.getEfficacy().contains(keyword)) {
            score += 2.0;
        }
        if (med.getIngredients() != null && med.getIngredients().contains(keyword)) {
            score += 1.0;
        }
        return score;
    }

    /**
     * RRF融合算法
     */
    private static final int RRF_K = 60;
    private static final double VECTOR_WEIGHT = 0.6;
    private static final double KEYWORD_WEIGHT = 0.4;

    private List<Long> rrfFusion(Map<Long, Double> vectorScores, 
                                  Map<Long, Double> keywordScores, 
                                  int topK) {
        Map<Long, Double> fusedScores = new HashMap<>();

        // 获取排名列表
        List<Long> vectorRanked = vectorScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();

        List<Long> keywordRanked = keywordScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .toList();

        // 计算RRF得分
        Set<Long> allIds = new HashSet<>();
        allIds.addAll(vectorRanked);
        allIds.addAll(keywordRanked);

        for (Long id : allIds) {
            double rrfScore = 0;

            int vectorRank = vectorRanked.indexOf(id);
            if (vectorRank >= 0) {
                rrfScore += VECTOR_WEIGHT / (RRF_K + vectorRank + 1);
            }

            int keywordRank = keywordRanked.indexOf(id);
            if (keywordRank >= 0) {
                rrfScore += KEYWORD_WEIGHT / (RRF_K + keywordRank + 1);
            }

            fusedScores.put(id, rrfScore);
        }

        return fusedScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .limit(topK)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }

    // ==================== 评测指标计算 ====================

    /**
     * 计算检索指标
     */
    private RetrievalMetrics calculateMetrics(List<Long> retrieved, 
                                               Set<Long> relevant, 
                                               int k, 
                                               long latencyMs) {
        // 截取前k个结果
        List<Long> topK = retrieved.subList(0, Math.min(k, retrieved.size()));

        // 计算命中数
        Set<Long> retrievedSet = new HashSet<>(topK);
        Set<Long> relevantSet = new HashSet<>(relevant);

        int hitCount = 0;
        for (Long id : retrievedSet) {
            if (relevantSet.contains(id)) {
                hitCount++;
            }
        }

        // Precision@K
        double precision = retrieved.isEmpty() ? 0 : (double) hitCount / k;

        // Recall@K
        double recall = relevant.isEmpty() ? 0 : (double) hitCount / relevant.size();

        // F1@K
        double f1 = (precision + recall) == 0 ? 0 : 2 * precision * recall / (precision + recall);

        // NDCG@K
        double ndcg = calculateNDCG(topK, relevantSet, k);

        // MRR (Mean Reciprocal Rank)
        double mrr = calculateMRR(topK, relevantSet);

        // MAP (Average Precision)
        double avgPrecision = calculateAveragePrecision(topK, relevantSet);

        return new RetrievalMetrics(precision, recall, f1, ndcg, mrr, avgPrecision, latencyMs);
    }

    /**
     * 计算NDCG (Normalized Discounted Cumulative Gain)
     */
    private double calculateNDCG(List<Long> retrieved, Set<Long> relevant, int k) {
        double dcg = 0;
        for (int i = 0; i < Math.min(k, retrieved.size()); i++) {
            if (relevant.contains(retrieved.get(i))) {
                dcg += 1.0 / (Math.log(i + 2) / Math.log(2)); // i+2因为从1开始计
            }
        }

        // 计算IDCG (理想情况)
        double idcg = 0;
        int relevantCount = Math.min(k, relevant.size());
        for (int i = 0; i < relevantCount; i++) {
            idcg += 1.0 / (Math.log(i + 2) / Math.log(2));
        }

        return idcg == 0 ? 0 : dcg / idcg;
    }

    /**
     * 计算MRR (Mean Reciprocal Rank)
     */
    private double calculateMRR(List<Long> retrieved, Set<Long> relevant) {
        for (int i = 0; i < retrieved.size(); i++) {
            if (relevant.contains(retrieved.get(i))) {
                return 1.0 / (i + 1);
            }
        }
        return 0;
    }

    /**
     * 计算AP (Average Precision)
     */
    private double calculateAveragePrecision(List<Long> retrieved, Set<Long> relevant) {
        if (relevant.isEmpty()) return 0;

        double sumPrecision = 0;
        int hitCount = 0;

        for (int i = 0; i < retrieved.size(); i++) {
            if (relevant.contains(retrieved.get(i))) {
                hitCount++;
                sumPrecision += (double) hitCount / (i + 1);
            }
        }

        return hitCount == 0 ? 0 : sumPrecision / relevant.size();
    }

    // ==================== 结果分析 ====================

    /**
     * 分析对比结果
     */
    private ComparisonResult analyzeResults(List<EvaluationResult> results) {
        Map<String, Object> analysis = new HashMap<>();

        // 找出最佳模式
        EvaluationResult bestPrecision = results.stream()
            .max(Comparator.comparing(r -> r.getAverageMetrics().getPrecisionAt10()))
            .orElse(results.get(0));

        EvaluationResult bestRecall = results.stream()
            .max(Comparator.comparing(r -> r.getAverageMetrics().getRecallAt10()))
            .orElse(results.get(0));

        EvaluationResult bestF1 = results.stream()
            .max(Comparator.comparing(r -> r.getAverageMetrics().getF1At10()))
            .orElse(results.get(0));

        EvaluationResult fastest = results.stream()
            .min(Comparator.comparing(r -> r.getAverageMetrics().getLatencyMs()))
            .orElse(results.get(0));

        analysis.put("bestPrecision", bestPrecision.getMode().getDescription());
        analysis.put("bestRecall", bestRecall.getMode().getDescription());
        analysis.put("bestF1", bestF1.getMode().getDescription());
        analysis.put("fastest", fastest.getMode().getDescription());

        // 计算提升百分比
        double hybridF1 = results.stream()
            .filter(r -> r.getMode() == RetrievalMode.HYBRID)
            .findFirst()
            .map(r -> r.getAverageMetrics().getF1At10())
            .orElse(0.0);

        double vectorF1 = results.stream()
            .filter(r -> r.getMode() == RetrievalMode.VECTOR_ONLY)
            .findFirst()
            .map(r -> r.getAverageMetrics().getF1At10())
            .orElse(0.0);

        double keywordF1 = results.stream()
            .filter(r -> r.getMode() == RetrievalMode.KEYWORD_ONLY)
            .findFirst()
            .map(r -> r.getAverageMetrics().getF1At10())
            .orElse(0.0);

        double hybridVsVector = vectorF1 == 0 ? 0 : (hybridF1 - vectorF1) / vectorF1 * 100;
        double hybridVsKeyword = keywordF1 == 0 ? 0 : (hybridF1 - keywordF1) / keywordF1 * 100;

        analysis.put("hybridVsVectorImprovement", String.format("%.2f%%", hybridVsVector));
        analysis.put("hybridVsKeywordImprovement", String.format("%.2f%%", hybridVsKeyword));

        // 生成建议
        StringBuilder recommendation = new StringBuilder();
        recommendation.append("【评测结论】\n\n");
        recommendation.append("1. 混合检索 vs 纯向量: ").append(String.format("%.2f%%", hybridVsVector)).append("\n");
        recommendation.append("2. 混合检索 vs 纯关键词: ").append(String.format("%.2f%%", hybridVsKeyword)).append("\n");
        recommendation.append("3. 最佳F1模式: ").append(bestF1.getMode().getDescription()).append("\n");
        recommendation.append("4. 最优延迟模式: ").append(fastest.getMode().getDescription()).append("\n\n");
        recommendation.append("【优化建议】\n");
        
        if (hybridF1 > vectorF1 && hybridF1 > keywordF1) {
            recommendation.append("- 推荐使用混合检索方案\n");
        }
        if (fastest.getMode() != bestF1.getMode()) {
            recommendation.append("- 如对延迟敏感，可考虑").append(fastest.getMode().getDescription()).append("\n");
        }

        return new ComparisonResult(results, analysis, recommendation.toString());
    }

    // ==================== 评测数据集管理 ====================

    /**
     * 添加标注数据
     */
    public void addGroundTruth(String query, Set<Long> relevantMedicineIds) {
        EVAL_QUERIES.put(query, relevantMedicineIds);
    }

    /**
     * 批量添加标注数据
     */
    public void addGroundTruthBatch(Map<String, Set<Long>> groundTruth) {
        EVAL_QUERIES.putAll(groundTruth);
    }

    /**
     * 获取当前评测数据集大小
     */
    public int getDatasetSize() {
        return EVAL_QUERIES.size();
    }

    /**
     * 导出评测报告
     */
    public String exportReport(ComparisonResult result) {
        StringBuilder report = new StringBuilder();
        report.append("=" .repeat(60)).append("\n");
        report.append("         药品检索系统评测报告\n");
        report.append("=" .repeat(60)).append("\n\n");

        // 各模式指标对比表
        report.append("【各模式指标对比】\n");
        report.append(String.format("%-25s %-8s %-8s %-8s %-8s %-8s %s\n",
            "模式", "P@10", "R@10", "F1@10", "NDCG", "MRR", "延迟(ms)"));
        report.append("-".repeat(80)).append("\n");

        for (EvaluationResult er : result.getResults()) {
            RetrievalMetrics m = er.getAverageMetrics();
            report.append(String.format("%-25s %.4f   %.4f   %.4f   %.4f   %.4f   %d\n",
                er.getMode().getDescription(),
                m.getPrecisionAt10(),
                m.getRecallAt10(),
                m.getF1At10(),
                m.getNdcgAt10(),
                m.getMrr(),
                m.getLatencyMs()));
        }

        report.append("\n");

        // 分析结论
        report.append("【分析结论】\n");
        for (Map.Entry<String, Object> entry : result.getAnalysis().entrySet()) {
            report.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue()));
        }

        report.append("\n");
        report.append(result.getRecommendation());

        report.append("\n\n");
        report.append("=" .repeat(60)).append("\n");
        report.append("报告生成时间: ").append(new java.util.Date()).append("\n");

        return report.toString();
    }
}
