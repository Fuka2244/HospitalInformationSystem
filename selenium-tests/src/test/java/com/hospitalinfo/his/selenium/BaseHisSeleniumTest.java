package com.hospitalinfo.his.selenium;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class BaseHisSeleniumTest {

    protected WebDriver driver;
    protected WebDriverWait wait;

    protected final String baseUrl = System.getProperty("his.baseUrl", "http://127.0.0.1:5173");
    protected final String patientAccount = System.getProperty("his.patient.account", "13800000001");
    protected final String patientPassword = System.getProperty("his.patient.password", "123456");
    protected final Duration timeout = Duration.ofSeconds(Long.parseLong(System.getProperty("his.timeout.seconds", "15")));

    @BeforeEach
    void setUp() {
        String browser = System.getProperty("browser", "chrome").toLowerCase();

        if ("edge".equals(browser)) {
            WebDriverManager.edgedriver().setup();
            EdgeOptions options = new EdgeOptions();
            options.addArguments("--window-size=1440,900");
            driver = new EdgeDriver(options);
        } else {
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--window-size=1440,900");
            driver = new ChromeDriver(options);
        }

        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(20));
        driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(10));
        wait = new WebDriverWait(driver, timeout);
    }

    @AfterEach
    void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    protected void open(String path) {
        String url = baseUrl + path;
        try {
            driver.get(url);
            waitForPageReady();
        } catch (TimeoutException e) {
            throw new AssertionError("打开页面超时：" + url + "。请确认前端服务已启动，并且地址可访问。", e);
        }
    }

    protected void waitForPageReady() {
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));
    }

    protected WebElement visible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected List<WebElement> all(By locator) {
        return driver.findElements(locator);
    }

    protected void click(By locator) {
        wait.until(ExpectedConditions.elementToBeClickable(locator)).click();
    }

    protected void type(By locator, String value) {
        WebElement element = visible(locator);
        element.clear();
        element.sendKeys(value);
    }

    protected void loginAsPatient() {
        open("/login");
        type(By.cssSelector("input[placeholder*='手机号'], input[placeholder*='账号']"), patientAccount);
        type(By.cssSelector("input[type='password']"), patientPassword);
        click(By.cssSelector(".submit-btn"));
        try {
            wait.until(webDriver -> !webDriver.getCurrentUrl().contains("/login"));
        } catch (TimeoutException e) {
            throw new AssertionError("患者登录超时，请确认后端服务已启动、测试账号正确，并且登录接口可用。账号：" + patientAccount, e);
        }
    }

    protected void assertOnPage(String path) {
        wait.until(webDriver -> webDriver.getCurrentUrl().contains(path));
        assertTrue(driver.getCurrentUrl().contains(path), "当前页面应包含路径：" + path);
    }

    protected boolean hasRows() {
        return !all(By.cssSelector(".el-table__body-wrapper tbody tr")).isEmpty();
    }
}
