package com.hospitalinfo.his.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("F12 医疗报告模块")
class MedicalReportTest extends BaseHisSeleniumTest {

    @Test
    @DisplayName("F12-01 未登录访问报告页，应跳转登录页")
    void shouldRedirectToLoginWhenAnonymousVisitsReport() {
        open("/report");

        wait.until(webDriver -> webDriver.getCurrentUrl().contains("/login"));
        assertOnPage("/login");
    }

    @Test
    @DisplayName("F12-02 登录后访问报告页，应显示报告列表")
    void shouldShowReportListAfterLogin() {
        loginAsPatient();
        open("/report");

        // 等待页面容器加载
        visible(By.cssSelector(".report-container"));
        
        // 等待表格或空数据出现（API 可能有延迟）
        wait.until(webDriver -> 
            !webDriver.findElements(By.cssSelector(".el-table")).isEmpty() ||
            !webDriver.findElements(By.cssSelector(".el-table__empty-text")).isEmpty()
        );

        assertTrue(hasRows() || !all(By.cssSelector(".el-table__empty-text")).isEmpty(),
                "报告页应显示报告列表或暂无数据提示");
    }

    @Test
    @DisplayName("F12-03 点击 AI 生成报告，应打开生成报告弹窗")
    void shouldOpenGenerateReportDialog() {
        loginAsPatient();
        open("/report");

        click(By.xpath("//button[contains(.,'AI') and contains(.,'报告')]"));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".el-dialog")));

        assertTrue(driver.findElement(By.cssSelector(".el-dialog")).isDisplayed(), "生成报告弹窗应显示");
    }

    @Test
    @DisplayName("F12-04 未选择病历时，生成报告按钮应不可用")
    void shouldDisableGenerateButtonWhenNoRecordSelected() {
        loginAsPatient();
        open("/report");

        click(By.xpath("//button[contains(.,'AI') and contains(.,'报告')]"));
        WebElement generateButton = visible(By.cssSelector(".el-dialog__footer .el-button--primary"));

        assertFalse(generateButton.isEnabled(), "未选择病历时生成按钮应不可用");
    }

    @Test
    @DisplayName("F12-05 有报告数据时，查看详情按钮应打开报告详情弹窗")
    void shouldOpenReportDetailWhenReportExists() {
        loginAsPatient();
        open("/report");
        visible(By.cssSelector(".el-table"));

        List<WebElement> viewButtons = all(By.xpath("//button[contains(.,'查看')]"));
        if (viewButtons.isEmpty()) {
            assertTrue(all(By.cssSelector(".el-table__empty-text")).size() > 0,
                    "无报告数据时应显示空数据提示");
            return;
        }

        viewButtons.get(0).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".el-dialog")));

        assertTrue(driver.findElement(By.cssSelector(".el-dialog")).isDisplayed(), "报告详情弹窗应显示");
    }

    @Test
    @DisplayName("F12-06 有报告数据时，应显示导出 PDF 操作入口")
    void shouldShowExportPdfActionWhenReportExists() {
        loginAsPatient();
        open("/report");
        
        // 等待页面加载
        visible(By.cssSelector(".report-container"));
        
        // 等待表格加载（可能有数据或显示空数据提示）
        wait.until(webDriver -> 
            !webDriver.findElements(By.cssSelector(".el-table")).isEmpty() ||
            !webDriver.findElements(By.cssSelector(".el-table__empty-text")).isEmpty()
        );

        List<WebElement> exportButtons = all(By.xpath("//button[contains(.,'PDF')]"));
        boolean hasEmptyText = !all(By.cssSelector(".el-table__empty-text")).isEmpty();
        
        // 有报告时应显示 PDF 按钮，无报告时应显示空数据提示
        assertTrue(!exportButtons.isEmpty() || hasEmptyText,
                "有报告时应显示 PDF 导出入口，无报告时应显示空数据提示");
    }
}
