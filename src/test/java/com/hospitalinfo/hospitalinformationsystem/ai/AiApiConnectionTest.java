package com.hospitalinfo.hospitalinformationsystem.ai;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI API 连接测试
 * 测试通义千问API是否能正常调用
 * 
 * 运行前确保：
 * 1. 已配置环境变量 AI_QWEN_API_KEY
 * 2. 网络连接正常
 * 
 * 运行方式：
 * - 命令行: mvn test -Dtest=AiApiConnectionTest
 * - IDEA: 右键此类 → Run
 */
@SpringBootTest
@DisplayName("AI API连接测试")
class AiApiConnectionTest {

    @Autowired
    private ChatLanguageModel chatModel;

    @Test
    @DisplayName("测试AI API能否正常调用")
    void testAiApiConnection() {
        // 发送一个简单的问题
        String question = "请回答：1+1等于几？";
        
        // 调用AI API
        String response = chatModel.generate(question);
        
        // 验证响应
        assertNotNull(response, "AI响应不应为null");
        assertFalse(response.isEmpty(), "AI响应不应为空");
        
        // 打印响应结果
        System.out.println("\n========================================");
        System.out.println("提问: " + question);
        System.out.println("回答: " + response);
        System.out.println("========================================\n");
        
        System.out.println("✅ AI API连接成功！");
    }
}
