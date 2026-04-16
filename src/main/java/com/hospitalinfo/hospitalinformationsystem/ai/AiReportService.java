package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.ReportGenerationResult;
import com.hospitalinfo.hospitalinformationsystem.entity.MedicalRecord;
import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI医疗报告生成服务
 * 基于Chain-of-Thought + RAG生成结构化医疗报告
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiReportService {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的医疗报告AI生成助手。你需要根据患者的病历数据、检查结果和历史记录，生成结构化的医疗报告。

            请按照以下思维链(Chain-of-Thought)步骤进行分析，并详细记录你的思考过程。在分析时，请直接引用病历中的具体数据，让思维过程清晰可见：

            步骤1：分析主诉和现病史
            - 提取主诉中的关键症状（如：患者主诉"头痛3天"）
            - 分析症状的持续时间、严重程度和发展趋势
            - 引用现病史中的描述，说明症状如何演变

            步骤2：结合检查结果分析
            - 列出关键检查结果及其异常值（如：血压145/95mmHg）
            - 分析这些异常结果与主诉症状的关系
            - 判断可能的疾病类型和范围

            步骤3：参考历史记录
            - 回顾患者的历史就诊记录，列出关键历史信息（如：2个月前诊断为高血压）
            - 判断是否存在慢性病、复发倾向或相关疾病史
            - 分析本次病情与历史病情的关联性

            步骤4：综合分析诊断
            - 综合主诉、现病史、检查结果、历史记录的所有信息
            - 得出最可能的诊断结论
            - 明确疾病名称、分型（如有）和严重程度
            - 说明得出此诊断的理由和依据

            步骤5：提出治疗方案和建议
            - 基于诊断结果，列出具体的治疗措施
            - 详细的用药建议（药物名称、用法用量、疗程）
            - 具体的康复建议（生活注意事项、饮食建议、复查计划）

            重要要求：
            1. 每个步骤都要引用病历中的具体数据，不要泛泛而谈
            2. 思维链要体现推理的连贯性和逻辑性
            3. 分析要清晰、具体、有依据

            你必须严格按照以下JSON格式返回结果，不要包含任何其他文字说明：
            {
                "summary": "病情摘要(包含主诉、关键症状、检查异常)",
                "diagnosis": "诊断结论(包含疾病名称、分型、严重程度)",
                "treatment": "治疗方案(包含用药建议、治疗措施、疗程)",
                "recommendation": "康复建议(包含生活注意事项、复查计划、饮食建议)",
                "thoughtChain": "详细的思维链分析过程，每个步骤都要有详细说明，引用具体数据"
            }
            """;

    /**
     * 生成医疗报告
     */
    public ReportGenerationResult generateReport(MedicalRecord medicalRecord,
                                                  String examinationData,
                                                  String historyContext) {
        String userPrompt = String.format("""
                【病历数据】
                主诉: %s
                现病史: %s
                当前诊断: %s
                治疗方案: %s
                就诊时间: %s
                
                【检查结果】
                %s
                
                【历史记录】
                %s
                
                请按照思维链步骤分析，生成结构化医疗报告。
                """,
                medicalRecord.getChiefComplaint(),
                medicalRecord.getPresentIllness(),
                medicalRecord.getDiagnosis(),
                medicalRecord.getTreatmentPlan(),
                medicalRecord.getVisitDate(),
                examinationData != null ? examinationData : "无",
                historyContext != null ? historyContext : "无历史记录");

        String response = chatModel.generate(SYSTEM_PROMPT + "\n\n" + userPrompt);
        log.info("AI报告生成原始响应: {}", response);

        return parseReportResult(response);
    }

    private ReportGenerationResult parseReportResult(String response) {
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
            return objectMapper.readValue(json, ReportGenerationResult.class);
        } catch (Exception e) {
            log.error("解析AI报告结果失败: {}", e.getMessage());
            ReportGenerationResult fallback = new ReportGenerationResult();
            fallback.setSummary("AI报告生成异常");
            fallback.setDiagnosis("请医生手动填写诊断");
            fallback.setTreatment("请医生手动填写治疗方案");
            fallback.setRecommendation("请医生手动填写建议");
            return fallback;
        }
    }
}
