package com.hospitalinfo.his.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("F10 AI 药品推荐模块")
class AiMedicineTest extends BaseHisSeleniumTest {

    @Test
    @DisplayName("F10-01 AI 药品区域应正常显示欢迎消息")
    void shouldShowAiMedicinePanel() {
        loginAsPatient();
        open("/medicine");

        visible(By.cssSelector(".ai-card"));
        assertTrue(driver.getPageSource().contains("AI"), "页面应包含 AI 药品推荐区域");
    }

    @Test
    @DisplayName("F10-02 空症状输入时发送按钮应不可用")
    void shouldDisableSendButtonWhenAiInputIsEmpty() {
        loginAsPatient();
        open("/medicine");

        WebElement sendButton = visible(By.cssSelector(".ai-card .chat-input-area button"));

        assertFalse(sendButton.isEnabled(), "空输入时发送按钮应不可用");
    }

    @Test
    @DisplayName("F10-03 输入常见症状后可以提交 AI 药品对话")
    void shouldSubmitAiMedicineQuestion() {
        loginAsPatient();
        open("/medicine");

        type(By.cssSelector(".ai-card .chat-input-area input"), "发热咳嗽可以吃什么药");
        click(By.cssSelector(".ai-card .chat-input-area button"));

        visible(By.cssSelector(".chat-message.user"));
        assertTrue(driver.getPageSource().contains("发热咳嗽"), "用户输入内容应显示在对话区域");
    }

    @Test
    @DisplayName("F10-04 特殊字符输入不应导致页面崩溃")
    void shouldHandleInvalidAiMedicineInput() {
        loginAsPatient();
        open("/medicine");

        type(By.cssSelector(".ai-card .chat-input-area input"), "abc123@@@");
        click(By.cssSelector(".ai-card .chat-input-area button"));

        visible(By.cssSelector(".medicine-container"));
        assertFalse(driver.getPageSource().isBlank(), "无意义输入不应导致页面空白");
    }
}
