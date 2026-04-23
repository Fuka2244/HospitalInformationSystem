package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingChatResponse;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingExplanation;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
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

    /** 多轮费用解释对话的System Prompt */
    private static final String BILLING_CHAT_SYSTEM_PROMPT = """
            你是一个专业、友好的医疗费用解释AI助手。系统会提供当前患者的费用明细数据，你需要基于这些真实数据直接回答患者的问题。

            ## 回复规则：
            1. 基于提供的费用明细数据直接回答患者的问题，不要先追问。
            2. 如果患者的问题比较模糊，可以先给出整体费用分析，再询问是否想了解某方面的细节。
            3. 语气亲切自然，像一位专业的收费咨询员。
            4. 解释要通俗易懂，避免过多专业术语。
            5. 如果有不合理的费用，要指出。
            6. 给出节省费用的建议。

            ## 回复格式：
            你必须在回复的最后附上一个JSON块来指示对话状态：
            - 如果患者可能还有后续问题，标记为未完成：
            ```json
            {"completed": false}
            ```
            - 如果已经充分回答了问题，标记为已完成：
            ```json
            {"completed": true}
            ```
            """;

    /**
     * 多轮对话式费用解释：基于数据库费用明细直接回答，支持后续追问
     */
    public BillingChatResponse billingChat(String userMessage, List<ChatMessageDto> history, List<Billing> billings) {
        // 构建费用明细上下文
        BigDecimal total = billings.stream()
                .map(Billing::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder detail = new StringBuilder();
        for (Billing b : billings) {
            detail.append(String.format("- 类型: %s | 项目: %s | 金额: %.2f元 | 状态: %s | 说明: %s\n",
                    b.getItemType(), b.getItemName(), b.getAmount(),
                    b.getStatus() == 1 ? "已支付" : (b.getStatus() == 2 ? "已退款" : "未支付"),
                    b.getDescription()));
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(BILLING_CHAT_SYSTEM_PROMPT).append("\n\n");
        promptBuilder.append("===当前患者费用明细===\n");
        promptBuilder.append(detail);
        promptBuilder.append(String.format("总计：%.2f元\n\n", total));

        // 添加历史对话
        if (history != null && !history.isEmpty()) {
            promptBuilder.append("===对话历史===\n");
            for (ChatMessageDto msg : history) {
                if ("user".equals(msg.getRole())) {
                    promptBuilder.append("患者：").append(msg.getContent()).append("\n");
                } else if ("assistant".equals(msg.getRole())) {
                    promptBuilder.append("费用助手：").append(msg.getContent()).append("\n");
                }
            }
            promptBuilder.append("\n");
        }

        // 添加当前用户消息
        promptBuilder.append("患者：").append(userMessage).append("\n\n");
        promptBuilder.append("请基于以上费用明细回答患者的问题。记住在回复最后附上JSON状态块。\n");

        String response = chatModel.generate(promptBuilder.toString());
        log.info("费用解释对话AI原始响应: {}", response);

        return parseBillingChatResponse(response, userMessage, history, billings);
    }

    /**
     * 解析费用解释对话AI的回复
     */
    private BillingChatResponse parseBillingChatResponse(String response, String userMessage, List<ChatMessageDto> history, List<Billing> billings) {
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
            log.warn("解析费用对话状态失败，默认为未完成: {}", e.getMessage());
        }

        // 清理回复文本中可能残留的JSON标记
        replyText = replyText.replaceAll("```\\w*\\s*", "").trim();
        if (replyText.endsWith("```")) {
            replyText = replyText.substring(0, replyText.length() - 3).trim();
        }

        if (!completed) {
            return BillingChatResponse.ongoing(replyText);
        }

        // 信息收集完毕，进行费用解释
        String fullQuestion = buildFullQuestionFromHistory(userMessage, history);
        log.info("费用对话完成，综合问题: {}", fullQuestion);

        BillingExplanation explanation = explainBilling(billings, fullQuestion);
        return BillingChatResponse.completed(replyText, explanation);
    }

    /**
     * 从对话历史中提取综合问题
     */
    private String buildFullQuestionFromHistory(String currentUserMessage, List<ChatMessageDto> history) {
        StringBuilder questionBuilder = new StringBuilder();
        if (history != null) {
            for (ChatMessageDto msg : history) {
                if ("user".equals(msg.getRole())) {
                    questionBuilder.append(msg.getContent()).append("；");
                }
            }
        }
        questionBuilder.append(currentUserMessage);
        return questionBuilder.toString();
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
