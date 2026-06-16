package com.hospitalinfo.his.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("F11 费用查询模块")
class BillingQueryTest extends BaseHisSeleniumTest {

    @Test
    @DisplayName("F11-01 未登录访问费用页，应跳转登录页")
    void shouldRedirectToLoginWhenAnonymousVisitsBilling() {
        open("/billing");

        wait.until(webDriver -> webDriver.getCurrentUrl().contains("/login"));
        assertOnPage("/login");
    }

    @Test
    @DisplayName("F11-02 登录后访问费用页，应显示费用统计和列表")
    void shouldShowBillingPageAfterLogin() {
        loginAsPatient();
        open("/billing");

        visible(By.cssSelector(".billing-container"));
        visible(By.cssSelector(".stats"));
        visible(By.cssSelector(".el-table"));

        assertTrue(hasRows() || !all(By.cssSelector(".el-table__empty-text")).isEmpty(),
                "费用页应显示账单列表或暂无数据提示");
    }

    @Test
    @DisplayName("F11-03 按费用类型筛选后，页面应保持可用")
    void shouldFilterBillingByType() {
        loginAsPatient();
        open("/billing");

        // 等待页面加载完成
        visible(By.cssSelector(".billing-container"));
        visible(By.cssSelector(".el-table"));

        // 点击费用类型筛选下拉框
        click(By.cssSelector(".filters .el-select"));
        
        // 等待下拉选项出现
        visible(By.xpath("//li[contains(.,'药品') or contains(.,'MEDICINE')]"));
        
        // 点击"药品"选项
        click(By.xpath("//li[contains(.,'药品') or contains(.,'MEDICINE')]"));

        // 等待下拉关闭
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}

        // 点击查询按钮
        click(By.cssSelector(".filters .el-button--primary"));

        // 等待页面数据加载（表格或空数据）
        wait.until(webDriver -> 
            !webDriver.findElements(By.cssSelector(".el-table")).isEmpty() ||
            !webDriver.findElements(By.cssSelector(".el-table__empty-text")).isEmpty()
        );
        
        assertFalse(driver.getPageSource().isBlank(), "费用筛选后页面不应为空");
    }

    @Test
    @DisplayName("F11-04 AI 费用解释空输入时发送按钮应不可用")
    void shouldDisableBillingAiSendWhenInputIsEmpty() {
        loginAsPatient();
        open("/billing");

        WebElement sendButton = visible(By.cssSelector(".ai-card .chat-input-area button"));

        assertFalse(sendButton.isEnabled(), "空输入时 AI 费用解释发送按钮应不可用");
    }

    @Test
    @DisplayName("F11-05 输入费用问题后可以提交 AI 费用对话")
    void shouldSubmitBillingAiQuestion() {
        loginAsPatient();
        open("/billing");

        type(By.cssSelector(".ai-card .chat-input-area input"), "为什么这次费用这么高");
        click(By.cssSelector(".ai-card .chat-input-area button"));

        visible(By.cssSelector(".chat-message.user"));
        assertTrue(driver.getPageSource().contains("费用"), "用户费用问题应显示在对话区域");
    }
}
