package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingExplanation;
import com.hospitalinfo.hospitalinformationsystem.entity.Billing;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * AI费用解释服务
 * 分析费用构成并用自然语言解释
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiBillingService {

    private final ChatLanguageModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个医疗费用解释AI助手。根据用户提供的费用明细，用通俗易懂的语言解释各项费用的合理性和必要性。
            
            你必须严格按照以下JSON格式返回结果，不要包含任何其他文字说明：
            {
                "totalAmount": 总金额(数字),
                "breakdown": "费用构成分析(分项说明)",
                "explanation": "费用合理性解释",
                "suggestion": "节省建议(如有)"
            }
            
            注意事项：
            1. 解释要通俗易懂，避免过多专业术语
            2. 说明每项费用的必要性
            3. 如果有不合理的费用，要指出
            4. 给出节省费用的建议
            """;

    /**
     * 解释费用明细
     */
    public BillingExplanation explainBilling(List<Billing> billings, String userQuestion) {
        // 构建费用明细上下文
        BigDecimal total = billings.stream()
                .map(Billing::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder detail = new StringBuilder();
        for (Billing b : billings) {
            detail.append(String.format("- 类型: %s | 项目: %s | 金额: %.2f元 | 说明: %s\n",
                    b.getItemType(), b.getItemName(), b.getAmount(), b.getDescription()));
        }

        String userPrompt = String.format("""
                费用明细：
                %s
                总计：%.2f元
                
                患者问题：%s
                
                请解释这些费用。
                """, detail, total, userQuestion);

        String response = chatModel.generate(SYSTEM_PROMPT + "\n\n" + userPrompt);
        log.info("AI费用解释原始响应: {}", response);

        return parseExplanation(response, total);
    }

    private BillingExplanation parseExplanation(String response, BigDecimal total) {
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
            return objectMapper.readValue(json, BillingExplanation.class);
        } catch (Exception e) {
            log.error("解析AI费用解释失败: {}", e.getMessage());
            BillingExplanation fallback = new BillingExplanation();
            fallback.setTotalAmount(total);
            fallback.setBreakdown("解析异常");
            fallback.setExplanation("AI解释服务暂时不可用，请咨询收费窗口");
            fallback.setSuggestion("");
            return fallback;
        }
    }
}
