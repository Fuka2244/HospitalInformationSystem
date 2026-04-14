package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.MedicineRecommendation;
import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI药品推荐服务
 * 基于LangChain4j + Qwen，结合药品数据库实现RAG式推荐
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiMedicineService {

    private final ChatLanguageModel chatModel;
    private final MedicineMapper medicineMapper;
    private final ObjectMapper objectMapper;

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
     * 根据症状推荐药品
     */
    public List<MedicineRecommendation> recommendBySymptom(String symptom) {
        // 从数据库检索相关药品（模拟RAG检索阶段）
        String medicineContext = buildMedicineContext(symptom);

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
     * 构建药品上下文（关键词粗筛 + 全量供给LLM精排）
     * 这是RAG的简化实现：用数据库查询替代向量检索
     */
    private String buildMedicineContext(String symptom) {
        // 粗筛：按类别或关键词检索可能相关的药品
        List<Medicine> allMedicines = medicineMapper.selectList(
                new QueryWrapper<Medicine>().eq("status", 1));

        StringBuilder sb = new StringBuilder();
        for (Medicine med : allMedicines) {
            sb.append(String.format("""
                    - 药品名: %s | 通用名: %s | 分类: %s | 规格: %s
                      成分: %s
                      功效: %s
                      副作用: %s
                      禁忌: %s
                      价格: %.2f元
                    """, med.getName(), med.getGenericName(), med.getCategory(),
                    med.getSpecification(), med.getIngredients(), med.getEfficacy(),
                    med.getSideEffects(), med.getContraindications(), med.getPrice()));
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
