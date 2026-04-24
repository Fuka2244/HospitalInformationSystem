package com.hospitalinfo.hospitalinformationsystem.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.config.AiConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.MedicineChatResponse;
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
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI药品推荐服务
 * 基于LangChain4j + Qwen，实现分层索引 + 混合检索的RAG流程：
 * 1. 启动时加载缓存或构建药品向量索引（字段级Chunk拆分）
 * 2. 查询时：向量语义检索 + SQL关键词检索 → RRF融合 → LLM精排
 *
 * 索引结构：
 * - 摘要Chunk（L1）: 药名+功效，语义聚焦，用于快速粗筛
 * - 字段Chunk（L2）: 成分/副作用/禁忌/用法各独立chunk，精确匹配各维度
 * - 所有chunk通过medicineId metadata关联，检索后按药品聚合
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

    /** 向量检索返回的候选chunk数量（会多于候选药品数，因为一个药品有多个chunk） */
    private static final int TOP_K_CHUNKS = 30;

    /** RRF融合后最终返回的候选药品数量 */
    private static final int TOP_K_MEDICINES = 10;

    /** SQL关键词检索返回的候选药品数量 */
    private static final int KEYWORD_TOP_K = 10;

    /** RRF常数K */
    private static final int RRF_K = 60;

    /** DashScope embedding API单次批量上限 */
    private static final int EMBEDDING_BATCH_SIZE = 25;

    /** DashScope embedding API单条文本最大token数（安全截断字符数） */
    private static final int MAX_EMBEDDING_TEXT_LENGTH = 1500;

    /** TextSegment metadata中药品ID的key */
    private static final String META_MEDICINE_ID = "medicineId";
    /** TextSegment metadata中chunk字段类型的key */
    private static final String META_FIELD = "field";
    /** TextSegment metadata中药品名称的key */
    private static final String META_MEDICINE_NAME = "medicineName";

    /** Chunk字段类型常量 */
    private static final String FIELD_SUMMARY = "summary";
    private static final String FIELD_EFFICACY = "efficacy";
    private static final String FIELD_INGREDIENTS = "ingredients";
    private static final String FIELD_SIDE_EFFECTS = "side_effects";
    private static final String FIELD_CONTRAINDICATIONS = "contraindications";
    private static final String FIELD_DOSAGE = "dosage";

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
     * 每个药品拆分为多个chunk：摘要 + 各字段独立chunk
     */
    private void buildIndex() {
        log.info("开始构建药品向量索引（字段级Chunk拆分）...");
        long start = System.currentTimeMillis();

        List<Medicine> medicines = medicineMapper.selectList(null);
        if (medicines.isEmpty()) {
            log.warn("药品数据库为空，跳过向量索引");
            return;
        }

        // 为每个药品生成多个chunk
        List<TextSegment> allSegments = new ArrayList<>();
        for (Medicine med : medicines) {
            allSegments.addAll(toTextSegments(med));
        }
        log.info("共生成{}个chunk，对应{}个药品", allSegments.size(), medicines.size());

        // 分批向量化并存储（DashScope单次上限25条）
        for (int i = 0; i < allSegments.size(); i += EMBEDDING_BATCH_SIZE) {
            int end = Math.min(i + EMBEDDING_BATCH_SIZE, allSegments.size());
            List<TextSegment> batch = allSegments.subList(i, end);
            Response<List<Embedding>> batchResponse = embeddingModel.embedAll(batch);
            List<Embedding> batchEmbeddings = batchResponse.content();
            for (int j = 0; j < batch.size(); j++) {
                medicineEmbeddingStore.add(batchEmbeddings.get(j), batch.get(j));
            }
            log.info("向量化进度: {}/{}", end, allSegments.size());
        }

        long elapsed = System.currentTimeMillis() - start;
        log.info("药品向量索引构建完成，共索引{}个chunk，耗时{}ms", allSegments.size(), elapsed);

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
     * 将药品实体转为多个TextSegment（字段级Chunk拆分）
     * L1摘要chunk: 药名+功效，语义聚焦
     * L2字段chunk: 各字段独立，精确匹配各维度
     */
    private List<TextSegment> toTextSegments(Medicine med) {
        List<TextSegment> segments = new ArrayList<>();
        String medId = String.valueOf(med.getId());
        String medName = med.getName() != null ? med.getName() : "未知药品";

        // L1 摘要chunk - 药名+功效（核心语义，用于快速粗筛）
        // 只有药名+功效都为空时才跳过摘要chunk（否则向量无意义）
        String efficacy = truncate(med.getEfficacy(), 500);
        boolean hasName = med.getName() != null && !med.getName().isBlank();
        boolean hasEfficacy = med.getEfficacy() != null && !med.getEfficacy().isBlank();
        if (hasName || hasEfficacy) {
            String summaryText = String.format("药品名: %s | 通用名: %s | 分类: %s | 功效: %s",
                    medName, nullToEmpty(med.getGenericName()), nullToEmpty(med.getCategory()), efficacy);
            segments.add(buildChunk(summaryText, medId, medName, FIELD_SUMMARY));
        }

        // L2 功效chunk - 功效详细描述
        if (med.getEfficacy() != null && !med.getEfficacy().isBlank()) {
            String efficacyText = String.format("药品名: %s | 功效: %s", medName, med.getEfficacy());
            segments.add(buildChunk(efficacyText, medId, medName, FIELD_EFFICACY));
        }

        // L2 成分chunk
        if (med.getIngredients() != null && !med.getIngredients().isBlank()) {
            String ingredientsText = String.format("药品名: %s | 成分: %s", medName, med.getIngredients());
            segments.add(buildChunk(ingredientsText, medId, medName, FIELD_INGREDIENTS));
        }

        // L2 副作用chunk
        if (med.getSideEffects() != null && !med.getSideEffects().isBlank()) {
            String sideEffectsText = String.format("药品名: %s | 副作用: %s", medName, med.getSideEffects());
            segments.add(buildChunk(sideEffectsText, medId, medName, FIELD_SIDE_EFFECTS));
        }

        // L2 禁忌chunk
        if (med.getContraindications() != null && !med.getContraindications().isBlank()) {
            String contraindicationsText = String.format("药品名: %s | 禁忌: %s", medName, med.getContraindications());
            segments.add(buildChunk(contraindicationsText, medId, medName, FIELD_CONTRAINDICATIONS));
        }

        // L2 用法用量chunk
        if (med.getDosage() != null && !med.getDosage().isBlank()) {
            String dosageText = String.format("药品名: %s | 用法用量: %s | 规格: %s | 价格: %.2f元",
                    medName, med.getDosage(), nullToEmpty(med.getSpecification()), med.getPrice());
            segments.add(buildChunk(dosageText, medId, medName, FIELD_DOSAGE));
        }

        return segments;
    }

    /**
     * 构建单个chunk，附加metadata
     */
    private TextSegment buildChunk(String text, String medicineId, String medicineName, String field) {
        if (text.length() > MAX_EMBEDDING_TEXT_LENGTH) {
            text = text.substring(0, MAX_EMBEDDING_TEXT_LENGTH);
        }
        TextSegment segment = TextSegment.from(text);
        segment.metadata().put(META_MEDICINE_ID, medicineId);
        segment.metadata().put(META_MEDICINE_NAME, medicineName);
        segment.metadata().put(META_FIELD, field);
        return segment;
    }

    private String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen) + "..." : text;
    }

    private String nullToEmpty(String text) {
        return text != null ? text : "";
    }

    /**
     * 根据症状推荐药品（混合检索RAG流程：向量检索 + SQL关键词检索 → RRF融合 → LLM精排）
     */
    public List<MedicineRecommendation> recommendBySymptom(String symptom) {
        long totalStart = System.currentTimeMillis();

        long t1 = System.currentTimeMillis();
        String medicineContext = hybridRetrieve(symptom);
        long t2 = System.currentTimeMillis();
        log.info("混合检索耗时: {}ms", t2 - t1);

        String userPrompt = String.format("""
                当前药品数据库信息：
                %s
                
                患者症状描述：%s
                
                请推荐合适的药品。
                """, medicineContext, symptom);

        long t3 = System.currentTimeMillis();
        String response = chatModel.generate(SYSTEM_PROMPT + "\n\n" + userPrompt);
        long t4 = System.currentTimeMillis();
        log.info("LLM生成耗时: {}ms", t4 - t3);
        log.info("AI药品推荐总耗时: {}ms", t4 - totalStart);
        log.info("AI药品推荐原始响应: {}", response);

        return parseRecommendations(response);
    }

    /**
     * 混合检索：向量语义检索 + SQL关键词检索，RRF融合后返回候选药品上下文
     */
    private String hybridRetrieve(String symptom) {
        long t1 = System.currentTimeMillis();
        // 1. 向量语义检索
        Map<Long, Double> vectorScores = vectorSearch(symptom);
        long t2 = System.currentTimeMillis();
        log.info("向量检索命中{}个药品，耗时: {}ms", vectorScores.size(), t2 - t1);

        // 2. SQL关键词检索
        Map<Long, Double> keywordScores = keywordSearch(symptom);
        long t3 = System.currentTimeMillis();
        log.info("关键词检索命中{}个药品，耗时: {}ms", keywordScores.size(), t3 - t2);

        // 3. RRF融合
        Map<Long, Double> fusedScores = rrfFusion(vectorScores, keywordScores);

        if (fusedScores.isEmpty()) {
            log.warn("混合检索未找到相关药品，症状: {}", symptom);
            return "未找到相关药品信息";
        }

        // 4. 取Top-K药品，从数据库加载完整信息
        List<Long> topMedicineIds = fusedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(TOP_K_MEDICINES)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<Medicine> topMedicines = medicineMapper.selectBatchIds(topMedicineIds);
        // 按融合分数排序
        Map<Long, Double> finalScores = fusedScores;
        topMedicines.sort((a, b) -> Double.compare(
                finalScores.getOrDefault(b.getId(), 0.0),
                finalScores.getOrDefault(a.getId(), 0.0)));

        // 5. 拼装完整上下文
        StringBuilder sb = new StringBuilder();
        for (Medicine med : topMedicines) {
            double score = fusedScores.getOrDefault(med.getId(), 0.0);
            sb.append(String.format("- [综合评分: %.4f] %s（通用名: %s）| 分类: %s | 规格: %s | 价格: %.2f元\n",
                    score, med.getName(), med.getGenericName(), med.getCategory(),
                    med.getSpecification(), med.getPrice()));
            sb.append(String.format("  功效: %s\n", med.getEfficacy()));
            sb.append(String.format("  成分: %s\n", med.getIngredients()));
            sb.append(String.format("  副作用: %s\n", med.getSideEffects()));
            sb.append(String.format("  禁忌: %s\n", med.getContraindications()));
            sb.append(String.format("  用法用量: %s\n", med.getDosage()));
        }
        return sb.toString();
    }

    /**
     * 向量语义检索：检索所有chunk，按medicineId聚合取最高分
     */
    private Map<Long, Double> vectorSearch(String symptom) {
        long t1 = System.currentTimeMillis();
        Embedding queryEmbedding = embeddingModel.embed(symptom).content();
        long t2 = System.currentTimeMillis();
        log.info("查询向量化耗时: {}ms", t2 - t1);

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(TOP_K_CHUNKS)
                .minScore(0.4)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = medicineEmbeddingStore.search(searchRequest);
        long t3 = System.currentTimeMillis();
        log.info("向量搜索耗时: {}ms，命中chunk数: {}", t3 - t2, searchResult.matches().size());
        List<EmbeddingMatch<TextSegment>> matches = searchResult.matches();

        // 按medicineId聚合，取每个药品的最高分
        Map<Long, Double> medicineMaxScores = new LinkedHashMap<>();
        Map<Long, String> medicineFields = new LinkedHashMap<>();

        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            String medicineIdStr = segment.metadata().getString(META_MEDICINE_ID);
            String field = segment.metadata().getString(META_FIELD);
            if (medicineIdStr == null) continue;

            long medicineId = Long.parseLong(medicineIdStr);
            double score = match.score();

            // 对摘要chunk的得分加权（摘要更聚焦，语义匹配更可靠）
            double weightedScore = FIELD_SUMMARY.equals(field) ? score * 1.2 : score;

            medicineMaxScores.merge(medicineId, weightedScore, Math::max);
            medicineFields.merge(medicineId, field, (a, b) -> a + "," + b);
        }

        // 按分数排序
        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(medicineMaxScores.entrySet());
        sorted.sort(Map.Entry.<Long, Double>comparingByValue().reversed());

        // 转为有序Map并截取
        Map<Long, Double> result = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : sorted) {
            result.put(entry.getKey(), entry.getValue());
            if (result.size() >= TOP_K_MEDICINES) break;
        }

        if (!result.isEmpty()) {
            log.info("向量检索Top药品: {}", result.entrySet().stream()
                    .map(e -> String.format("药品ID=%d(%.3f)", e.getKey(), e.getValue()))
                    .collect(Collectors.joining(", ")));
        }

        return result;
    }

    /**
     * SQL关键词检索：在药名、通用名、功效、成分字段中模糊匹配
     * 返回按相关度排序的药品ID及其得分
     */
    private Map<Long, Double> keywordSearch(String symptom) {
        // 分词：简单按标点和空格切分
        String[] keywords = symptom.split("[，,。.、！!？?\\s]+");
        List<String> validKeywords = Arrays.stream(keywords)
                .filter(kw -> kw.length() >= 2)
                .distinct()
                .collect(Collectors.toList());

        if (validKeywords.isEmpty()) {
            return Collections.emptyMap();
        }

        // 对每个关键词在多字段中搜索，统计命中次数作为相关度
        Map<Long, Double> medicineScores = new LinkedHashMap<>();

        for (String keyword : validKeywords) {
            QueryWrapper<Medicine> wrapper = new QueryWrapper<>();
            wrapper.and(w -> w
                    .like("name", keyword)
                    .or().like("generic_name", keyword)
                    .or().like("efficacy", keyword)
                    .or().like("ingredients", keyword)
            );
            List<Medicine> matches = medicineMapper.selectList(wrapper);

            for (Medicine med : matches) {
                double score = 0.0;
                // 不同字段命中给予不同权重
                if (med.getName() != null && med.getName().contains(keyword)) score += 3.0;
                if (med.getGenericName() != null && med.getGenericName().contains(keyword)) score += 2.5;
                if (med.getEfficacy() != null && med.getEfficacy().contains(keyword)) score += 2.0;
                if (med.getIngredients() != null && med.getIngredients().contains(keyword)) score += 1.0;
                medicineScores.merge(med.getId(), score, Double::sum);
            }
        }

        // 按分数排序
        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(medicineScores.entrySet());
        sorted.sort(Map.Entry.<Long, Double>comparingByValue().reversed());

        Map<Long, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(sorted.size(), KEYWORD_TOP_K); i++) {
            result.put(sorted.get(i).getKey(), sorted.get(i).getValue());
        }

        if (!result.isEmpty()) {
            log.info("关键词检索命中: {}", result.entrySet().stream()
                    .map(e -> String.format("药品ID=%d(%.1f)", e.getKey(), e.getValue()))
                    .collect(Collectors.joining(", ")));
        }

        return result;
    }

    /**
     * RRF（Reciprocal Rank Fusion）融合向量检索和关键词检索的结果
     * RRF公式: score = Σ 1/(K + rank_i)，其中K为平滑常数
     */
    private Map<Long, Double> rrfFusion(Map<Long, Double> vectorScores, Map<Long, Double> keywordScores) {
        Map<Long, Double> fusedScores = new LinkedHashMap<>();

        // 向量检索排名
        List<Long> vectorRanked = vectorScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 关键词检索排名
        List<Long> keywordRanked = keywordScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 收集所有候选药品ID
        Set<Long> allMedicineIds = new LinkedHashSet<>();
        allMedicineIds.addAll(vectorRanked);
        allMedicineIds.addAll(keywordRanked);

        for (Long medicineId : allMedicineIds) {
            double rrfScore = 0.0;

            // 向量检索排名得分（权重0.6，语义检索更重要）
            int vectorRank = vectorRanked.indexOf(medicineId);
            if (vectorRank >= 0) {
                rrfScore += 0.6 / (RRF_K + vectorRank + 1);
            }

            // 关键词检索排名得分（权重0.4）
            int keywordRank = keywordRanked.indexOf(medicineId);
            if (keywordRank >= 0) {
                rrfScore += 0.4 / (RRF_K + keywordRank + 1);
            }

            fusedScores.put(medicineId, rrfScore);
        }

        // 按融合分数排序
        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(fusedScores.entrySet());
        sorted.sort(Map.Entry.<Long, Double>comparingByValue().reversed());

        Map<Long, Double> result = new LinkedHashMap<>();
        for (Map.Entry<Long, Double> entry : sorted) {
            result.put(entry.getKey(), entry.getValue());
        }

        log.info("RRF融合后Top药品: {}", sorted.stream().limit(5)
                .map(e -> String.format("药品ID=%d(%.6f)", e.getKey(), e.getValue()))
                .collect(Collectors.joining(", ")));

        return result;
    }

    /** 多轮药品推荐对话的System Prompt */
    private static final String MEDICINE_CHAT_SYSTEM_PROMPT = """
            你是一个专业、友好的药学AI助手。你的任务是通过对话逐步了解患者的症状和用药需求，然后为其推荐合适的药品。

            ## 对话流程规则：
            1. **第一步：询问症状** - 如果患者还没有描述症状，请温和地询问其症状。如果患者已经描述了症状，可以适当追问细节（如持续时间、是否过敏、是否在服用其他药物等），但不要过于啰嗦。
            2. **第二步：给出推荐** - 当症状信息收集足够后，进行药品推荐。

            ## 回复格式规则：
            你必须在每次回复的最后附上一个JSON块来指示对话状态，格式如下：

            当还在收集信息时（还需要继续对话）：
            ```json
            {"completed": false}
            ```

            当信息已收集完毕，准备给出推荐时：
            ```json
            {"completed": true}
            ```

            ## 重要注意事项：
            - 语气亲切自然，像一位专业的药师
            - 不要一次性问太多问题，每次只问1-2个关键问题
            - 如果患者描述的症状比较模糊，适当追问以明确
            - 如果患者提供了足够的信息，不要再追问，直接标记为completed
            - 你只需要负责收集信息，具体的药品推荐由后台完成，你不需要在对话中推荐药品
            - 每次回复必须以JSON块结尾，不要遗漏
            - 特别注意询问患者是否有药物过敏史，这是用药安全的关键信息
            """;

    /**
     * 多轮对话式药品推荐
     */
    public MedicineChatResponse medicineChat(String userMessage, List<ChatMessageDto> history) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(MEDICINE_CHAT_SYSTEM_PROMPT).append("\n\n");

        // 添加历史对话
        if (history != null && !history.isEmpty()) {
            promptBuilder.append("===对话历史===\n");
            for (ChatMessageDto msg : history) {
                if ("user".equals(msg.getRole())) {
                    promptBuilder.append("患者：").append(msg.getContent()).append("\n");
                } else if ("assistant".equals(msg.getRole())) {
                    promptBuilder.append("药学助手：").append(msg.getContent()).append("\n");
                }
            }
            promptBuilder.append("\n");
        }

        // 添加当前用户消息
        promptBuilder.append("患者：").append(userMessage).append("\n\n");
        promptBuilder.append("请根据以上对话，继续与患者沟通。记住在回复最后附上JSON状态块。\n");

        String response = chatModel.generate(promptBuilder.toString());
        log.info("药品推荐对话AI原始响应: {}", response);

        return parseMedicineChatResponse(response, userMessage, history);
    }

    /**
     * 解析药品推荐对话AI的回复
     */
    private MedicineChatResponse parseMedicineChatResponse(String response, String userMessage, List<ChatMessageDto> history) {
        boolean completed = false;
        String replyText = response;

        try {
            if (response.contains("```json")) {
                int lastJsonBlock = response.lastIndexOf("```json");
                String jsonPart = response.substring(lastJsonBlock + 7);
                jsonPart = jsonPart.substring(0, jsonPart.indexOf("```")).trim();

                var statusNode = objectMapper.readTree(jsonPart);
                if (statusNode.has("completed")) {
                    completed = statusNode.get("completed").asBoolean();
                }

                replyText = response.substring(0, lastJsonBlock).trim();
                replyText = replyText.replaceAll("```json\\s*\\{[^}]*}\\s*```", "").trim();
            } else if (response.contains("```")) {
                int lastBlock = response.lastIndexOf("```");
                String blockContent = response.substring(response.lastIndexOf("```", lastBlock - 1) + 3, lastBlock).trim();

                try {
                    var statusNode = objectMapper.readTree(blockContent);
                    if (statusNode.has("completed")) {
                        completed = statusNode.get("completed").asBoolean();
                    }
                } catch (Exception ignored) {}

                replyText = response.substring(0, response.lastIndexOf("```", lastBlock - 1)).trim();
                replyText = replyText.replaceAll("```\\s*\\{[^}]*}\\s*```", "").trim();
            }
        } catch (Exception e) {
            log.warn("解析药品对话状态失败，默认为未完成: {}", e.getMessage());
        }

        // 清理回复文本中可能残留的JSON标记
        replyText = replyText.replaceAll("```\\w*\\s*", "").trim();
        if (replyText.endsWith("```")) {
            replyText = replyText.substring(0, replyText.length() - 3).trim();
        }

        if (!completed) {
            return MedicineChatResponse.ongoing(replyText);
        }

        // 信息收集完毕，进行药品推荐
        String fullSymptom = buildFullSymptomFromHistory(userMessage, history);
        log.info("药品对话完成，综合症状描述: {}", fullSymptom);

        List<MedicineRecommendation> recommendations = recommendBySymptom(fullSymptom);
        return MedicineChatResponse.completed(replyText, recommendations);
    }

    /**
     * 从对话历史中提取综合症状描述
     */
    private String buildFullSymptomFromHistory(String currentUserMessage, List<ChatMessageDto> history) {
        StringBuilder symptomBuilder = new StringBuilder();
        if (history != null) {
            for (ChatMessageDto msg : history) {
                if ("user".equals(msg.getRole())) {
                    symptomBuilder.append(msg.getContent()).append("；");
                }
            }
        }
        symptomBuilder.append(currentUserMessage);
        return symptomBuilder.toString();
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
