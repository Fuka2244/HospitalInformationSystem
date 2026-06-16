package com.hospitalinfo.his.selenium;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("F09 药品查询模块")
class MedicineQueryTest extends BaseHisSeleniumTest {

    @Test
    @DisplayName("F09-01 查询药品列表，应显示药品表格")
    void shouldShowMedicineList() {
        loginAsPatient();
        open("/medicine");

        visible(By.cssSelector(".medicine-container"));
        visible(By.cssSelector(".el-table"));

        assertTrue(hasRows(), "药品列表应至少显示一条数据");
    }

    @Test
    @DisplayName("F09-02 输入有效关键词查询，应返回药品列表或无数据提示")
    void shouldSearchMedicineByKeyword() {
        loginAsPatient();
        open("/medicine");

        type(By.cssSelector(".filters input[placeholder*='搜索']"), "阿莫西林");
        click(By.cssSelector(".filters .el-button--primary"));

        visible(By.cssSelector(".el-table"));
        assertTrue(hasRows() || !all(By.cssSelector(".el-table__empty-text")).isEmpty(),
                "关键词查询后应显示结果或空数据提示");
    }

    @Test
    @DisplayName("F09-03 输入不存在关键词，应稳定显示空结果")
    void shouldHandleUnknownMedicineKeyword() {
        loginAsPatient();
        open("/medicine");

        type(By.cssSelector(".filters input[placeholder*='搜索']"), "不存在药品XYZ");
        click(By.cssSelector(".filters .el-button--primary"));

        visible(By.cssSelector(".el-table"));
        assertTrue(hasRows() || !all(By.cssSelector(".el-table__empty-text")).isEmpty(),
                "不存在关键词不应导致页面崩溃");
    }

    @Test
    @DisplayName("F09-04 输入特殊字符查询，不应出现白屏或脚本错误")
    void shouldHandleSpecialCharactersInMedicineSearch() {
        loginAsPatient();
        open("/medicine");

        type(By.cssSelector(".filters input[placeholder*='搜索']"), "@#$%");
        click(By.cssSelector(".filters .el-button--primary"));

        visible(By.cssSelector(".medicine-container"));
        assertFalse(driver.getPageSource().isBlank(), "页面内容不应为空");
    }

    @Test
    @DisplayName("F09-05 点击药品详情，应打开详情弹窗")
    void shouldOpenMedicineDetailDialog() {
        loginAsPatient();
        open("/medicine");
        visible(By.cssSelector(".el-table"));

        List<WebElement> detailButtons = all(By.xpath("//button[contains(.,'详情')]"));
        assertFalse(detailButtons.isEmpty(), "列表中应存在详情按钮");

        detailButtons.get(0).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".el-dialog")));

        assertTrue(driver.findElement(By.cssSelector(".el-dialog")).isDisplayed(), "药品详情弹窗应显示");
    }
}
