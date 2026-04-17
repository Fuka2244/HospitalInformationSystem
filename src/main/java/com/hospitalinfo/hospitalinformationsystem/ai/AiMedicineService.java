package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.config.AiConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.MedicineRecommendation;
import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI药品推荐服务
 * 基于LangChain4j + Qwen，实现真正的RAG流程：
 * 1. 启动时加载缓存或构建药品向量索引
 * 2. 查询时通过向量相似度检索Top-K药品
 * 3. 将检索到的药品上下文送LLM精排推荐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMedicineService implements CommandLineRunner {

    private final ChatLanguageModel chatModel;
    private final MedicineMapper medicineMapper;
    private final ObjectMapper objectMapper;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> medicineEmbeddingStore;
    private final AiConfig aiConfig;

    @Value("${ai.qwen.embedding.persist-path:medicine-embedding-store.json}")
    private String persistPath;

    /** 向量检索返回的候选药品数量 */
    private static final int TOP_K = 10;

    /** DashScope embedding API单次批量上限 */
    private static final int EMBEDDING_BATCH_SIZE = 25;

    /** DashScope embedding API单条文本最大token数（安全截断字符数） */
    private static final int MAX_EMBEDDING_TEXT_LENGTH = 1500;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的药学AI助手。根据用户描述的症状，结合药品数据库信息，推荐合适的非处方药物。
            
            你必须严格按照以下JSON数组格式返回结果，不要包含任何其他文字说明：
            [
                {
                    "medicineName": "药品名称",
                    "reason": "推荐理由(结合症状和药品功效)",
                    "dosage": "建议用法用量",
                    "precautions": "注意事项(包含禁忌和副作用提醒)"
                }
            ]
            
            注意事项：
            1. 药品必须从提供的药品列表中选择
            2. 推荐理由应结合药品的功效和用户症状
            3. 必须提醒用户注意禁忌和副作用
            4. 最多推荐3种药品
            5. 声明：AI推荐仅供参考，具体用药请遵医嘱
            """;

    /**
     * 应用启动时，若缓存已加载则跳过，否则构建索引并持久化
     */
    @Override
    public void run(String... args) {
        if (aiConfig.isEmbeddingStoreLoadedFromCache()) {
            log.info("药品向量索引已从缓存加载，跳过构建");
            return;
        }
        buildIndex();
    }

    /**
     * 构建药品向量索引并持久化到磁盘
     */
    private void buildIndex() {
        log.info("开始构建药品向量索引...");
        long start = System.currentTimeMillis();

        List<Medicine> medicines = medicineMapper.selectList(null);
        if (medicines.isEmpty()) {
            log.warn("药品数据库为空，跳过向量索引");
            return;
        }

        List<TextSegment> segments = medicines.stream()
                .map(this::toTextSegment)
                .collect(Collectors.toList());

        // 分批向量化并存储（DashScope单次上限25条）
        for (int i = 0; i < segments.size(); i += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(i + EMBEDDING_BATCH_SIZE, segments.size());
            List<TextSegment> batch = segments.subList(i, end);
            Response<List<Embedding>> batchResponse = embeddingModel.embedAll(batch);
            List<Embedding> batchEmbeddings = batchResponse.content();
            for (int j = 0; j < batch.size(); j++) {
                medicineEmbeddingStore.add(batchEmbeddings.get(j), batch.get(j));
            }
            log.info("向量化进度: {}/{}", end, segments.size());
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("药品向量索引构建完成，共索引{}条药品，耗时{}ms", medicines.size(), elapsed);

        // 持久化到磁盘
        persistToFile();
    }

    /**
     * 将向量索引序列化到磁盘文件
     */
    private void persistToFile() {
        if (!(medicineEmbeddingStore instanceof InMemoryEmbeddingStore<TextSegment> memoryStore)) {
            return;
        }
        try {
            memoryStore.serializeToFile(Paths.get(persistPath).toString());
            log.info("向量索引已缓存到: {}", Paths.get(persistPath).toAbsolutePath());
        } catch (Exception e) {
            log.warn("向量索引持久化失败（不影响运行）: {}", e.getMessage());
        }
    }

    /**
     * 将药品实体转为TextSegment（用于向量化）
     */
    private TextSegment toTextSegment(Medicine med) {
        String ingredients = truncate(med.getIngredients(), 100);
        String efficacy = truncate(med.getEfficacy(), 300);
        String sideEffects = truncate(med.getSideEffects(), 200);
        String contraindications = truncate(med.getContraindications(), 200);

        String text = String.format("""
                药品名: %s | 通用名: %s | 分类: %s | 规格: %s
                成分: %s
                功效: %s
                副作用: %s
                禁忌: %s
                价格: %.2f元""",
                med.getName(), med.getGenericName(), med.getCategory(),
                med.getSpecification(), ingredients, efficacy,
                sideEffects, contraindications, med.getPrice());

        if (text.length() > MAX_EMBEDDING_TEXT_LENGTH) {
            text = text.substring(0, MAX_EMBEDDING_TEXT_LENGTH);
        }

        TextSegment segment = TextSegment.from(text);
        segment.metadata().put("medicineId", String.valueOf(med.getId()));
        segment.metadata().put("medicineName", med.getName());
        return segment;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    /**
     * 根据症状推荐药品（RAG流程：向量检索 + LLM精排）
     */
    public List<MedicineRecommendation> recommendBySymptom(String symptom) {
        String medicineContext = retrieveByVector(symptom);

        String userPrompt = String.format("""
                当前药品数据库信息：
                %s
                
                患者症状描述：%s
                
                请推荐合适的药品。
                """, medicineContext, symptom);

        String response = chatModel.generate(SYSTEM_PROMPT + "\n\n" + userPrompt);
        log.info("AI药品推荐原始响应: {}", response);

        return parseRecommendations(response);
    }

    /**
     * 通过向量相似度检索最相关的药品
     */
    private String retrieveByVector(String symptom) {
        Embedding queryEmbedding = embeddingModel.embed(symptom).content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(TOP_K)
                .minScore(0.5)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = medicineEmbeddingStore.search(searchRequest);
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        if (matches.isEmpty()) {
            log.warn("向量检索未找到相关药品，症状: {}", symptom);
            return "未找到相关药品信息";
        }

        log.info("向量检索到{}条候选药品，相似度范围: [{}, {}]",
                matches.size(),
                matches.get(matches.size() - 1).score(),
                matches.get(0).score());

        StringBuilder sb = new StringBuilder();
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            sb.append(String.format("- [相似度: %.2f] %s\n", match.score(), segment.text()));
        }
        return sb.toString();
    }

    private List<MedicineRecommendation> parseRecommendations(String response) {
        try {
            String json = response;
            if (response.contains("```json")) {
                json = response.substring(response.indexOf("```json") + 7);
                json = json.substring(0, json.indexOf("```"));
            } else if (response.contains("```")) {
                json = response.substring(response.indexOf("```") + 3);
                json = json.substring(0, json.indexOf("```"));
            }
            json = json.trim();
            return objectMapper.readValue(json, new TypeReference<List<MedicineRecommendation>>() {});
        } catch (Exception e) {
            log.error("解析AI药品推荐结果失败: {}", e.getMessage());
            return List.of();
        }
    }
}
