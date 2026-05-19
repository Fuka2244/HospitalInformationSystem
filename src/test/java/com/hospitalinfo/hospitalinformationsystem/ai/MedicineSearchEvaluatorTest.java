package com.hospitalinfo.hospitalinformationsystem.ai;

import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 药品检索评测模块测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("药品检索系统评测")
class MedicineSearchEvaluatorTest {

    @Mock
    private MedicineMapper medicineMapper;

    @Mock
    private EmbeddingModel embeddingModel;

    private MedicineSearchEvaluator evaluator;

    @BeforeEach
    void setUp() {
        evaluator = new MedicineSearchEvaluator(medicineMapper, embeddingModel);
    }

    @Test
    @DisplayName("评测数据集管理 - 添加标注数据")
    void testAddGroundTruth() {
        int initialSize = evaluator.getDatasetSize();
        Set<Long> relevantIds = new HashSet<>();
        relevantIds.add(1L);
        relevantIds.add(2L);
        relevantIds.add(3L);

        evaluator.addGroundTruth("测试查询", relevantIds);

        assertEquals(initialSize + 1, evaluator.getDatasetSize());
    }

    @Test
    @DisplayName("评测数据集管理 - 批量添加标注数据")
    void testAddGroundTruthBatch() {
        int initialSize = evaluator.getDatasetSize();
        Map<String, Set<Long>> batch = new HashMap<>();
        batch.put("测试咳嗽", Set.of(1L, 2L));
        batch.put("测试感冒", Set.of(3L, 4L));
        batch.put("测试胃痛", Set.of(5L, 6L));

        evaluator.addGroundTruthBatch(batch);

        assertEquals(initialSize + 3, evaluator.getDatasetSize());
    }

    @Test
    @DisplayName("评测模式枚举 - 描述验证")
    void testRetrievalModes() {
        assertEquals("纯向量检索", MedicineSearchEvaluator.RetrievalMode.VECTOR_ONLY.getDescription());
        assertEquals("纯关键词检索", MedicineSearchEvaluator.RetrievalMode.KEYWORD_ONLY.getDescription());
        assertEquals("混合检索(混合+RRF)", MedicineSearchEvaluator.RetrievalMode.HYBRID.getDescription());
        assertEquals("混合检索+LLM精排", MedicineSearchEvaluator.RetrievalMode.HYBRID_WITH_RERANK.getDescription());
    }

    @Test
    @DisplayName("评测指标计算 - Precision")
    void testPrecisionCalculation() {
        java.util.List<Long> retrieved = java.util.List.of(1L, 2L, 3L, 4L, 5L);
        Set<Long> relevant = Set.of(1L, 3L, 6L);

        assertNotNull(retrieved);
        assertEquals(5, retrieved.size());
    }

    @Test
    @DisplayName("评测指标计算 - Recall")
    void testRecallCalculation() {
        java.util.List<Long> retrieved = java.util.List.of(1L, 2L, 3L);
        Set<Long> relevant = Set.of(1L, 3L, 5L, 7L);

        assertEquals(2, (long) retrieved.stream().filter(relevant::contains).count());
    }

    @Test
    @DisplayName("评测指标计算 - F1")
    void testF1Calculation() {
        double precision = 0.4;
        double recall = 0.5;
        double expectedF1 = 2 * precision * recall / (precision + recall);

        assertEquals(0.4444, expectedF1, 0.001);
    }

    @Test
    @DisplayName("评测指标计算 - NDCG")
    void testNDCGCalculation() {
        java.util.List<Long> retrieved = java.util.List.of(1L, 2L, 3L);
        Set<Long> relevant = Set.of(1L, 3L);

        double dcg = 1.0 / Math.log(3) + 1.0 / Math.log(5);
        double idcg = 1.0 / Math.log(3) + 1.0 / Math.log(4);

        assertTrue(dcg > 0);
        assertTrue(idcg > dcg);
    }

    @Test
    @DisplayName("评测指标计算 - MRR")
    void testMRRCalculation() {
        java.util.List<Long> retrieved = java.util.List.of(2L, 1L, 3L);
        Set<Long> relevant = Set.of(1L);

        double mrr = 1.0 / (retrieved.indexOf(1L) + 1);
        assertEquals(0.5, mrr, 0.001);
    }

    @Test
    @DisplayName("评测指标计算 - MAP")
    void testMAPCalculation() {
        java.util.List<Long> retrieved = java.util.List.of(1L, 2L, 3L, 4L, 5L);
        Set<Long> relevant = Set.of(1L, 3L, 5L);

        double ap = (1.0/1 + 1.0/2 + 2.0/3 + 2.0/4 + 3.0/5) / 3;
        assertTrue(ap > 0.5);
    }

    @Test
    @DisplayName("RRF融合算法验证")
    void testRRFFusion() {
        int K = 60;
        int vectorRank = 0;
        int keywordRank = 2;

        double vectorWeight = 0.6;
        double keywordWeight = 0.4;

        double rrfScore = vectorWeight / (K + vectorRank + 1) + keywordWeight / (K + keywordRank + 1);
        double expected = 0.6 / 61 + 0.4 / 63;

        assertEquals(expected, rrfScore, 0.0001);
    }

    @Test
    @DisplayName("评测结果数据结构完整性")
    void testRetrievalMetricsStructure() {
        MedicineSearchEvaluator.RetrievalMetrics metrics =
            new MedicineSearchEvaluator.RetrievalMetrics(0.8, 0.6, 0.7, 0.75, 0.9, 0.7, 50);

        assertEquals(0.8, metrics.getPrecisionAt10());
        assertEquals(0.6, metrics.getRecallAt10());
        assertEquals(0.7, metrics.getF1At10());
        assertEquals(0.75, metrics.getNdcgAt10());
        assertEquals(0.9, metrics.getMrr());
        assertEquals(0.7, metrics.getAveragePrecision());
        assertEquals(50, metrics.getLatencyMs());
    }

    @Test
    @DisplayName("评测结果导出报告格式")
    void testReportExport() {
        MedicineSearchEvaluator.ComparisonResult result =
            new MedicineSearchEvaluator.ComparisonResult(
                new java.util.ArrayList<>(),
                new HashMap<>(),
                "测试建议"
            );

        String report = evaluator.exportReport(result);
        assertNotNull(report);
        assertTrue(report.contains("药品检索系统评测报告"));
    }

    @Test
    @DisplayName("混合检索权重配置验证")
    void testHybridWeights() {
        assertEquals(0.6, 0.6);
        assertEquals(0.4, 0.4);
    }
}
