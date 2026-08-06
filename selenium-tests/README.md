# 学生 C Selenium 自动化测试说明

## 覆盖功能

本测试脚本覆盖学生 C 负责的 4 个功能点：

| 功能编号 | 功能名称 | 脚本文件 |
|---|---|---|
| F09 | 药品查询 | `MedicineQueryTest.java` |
| F10 | AI 药品推荐 | `AiMedicineTest.java` |
| F11 | 费用查询 | `BillingQueryTest.java` |
| F12 | 医疗报告 | `MedicalReportTest.java` |

## 测试方法

| 功能点 | 测试方法 | 说明 |
|---|---|---|
| F09 药品查询 | 等价类、边界值、错误推测 | 验证正常关键词、不存在关键词、特殊字符、详情弹窗 |
| F10 AI 药品推荐 | 等价类、边界值、错误推测 | 验证空输入、有效症状、无意义输入、AI 对话提交 |
| F11 费用查询 | 等价类、边界值、判定表 | 验证未登录拦截、登录后列表、费用类型筛选、AI 费用解释 |
| F12 医疗报告 | 等价类、边界值、判定表 | 验证未登录拦截、报告列表、生成弹窗、未选病历限制、详情和 PDF 入口 |

## 环境要求

- JDK 21
- Maven 3.6+
- Chrome 或 Edge
- 已启动后端服务：`http://localhost:8080/HIS`
- 已启动前端服务：`http://127.0.0.1:5173`
- 数据库中存在可登录的患者账号

## 运行前准备

1. 启动后端：

```bash
mvn spring-boot:run
```

2. 启动前端：

```bash
cd frontend
npm run dev
```

3. 确认可访问：

```text
http://127.0.0.1:5173
```

## 运行全部测试

进入 `selenium-tests` 目录：

```bash
cd selenium-tests
```

执行：

```bash
mvn test
```

## 指定测试账号

费用查询和医疗报告功能需要患者登录。默认脚本使用：

```text
账号：13800000001
密码：123456
```

如果你的数据库中患者账号不同，可以用 Maven 参数指定：

```bash
mvn test -Dhis.patient.account=你的手机号 -Dhis.patient.password=你的密码
```

## 指定前端地址

默认前端地址为：

```text
http://127.0.0.1:5173
```

如果端口不同：

```bash
mvn test -Dhis.baseUrl=http://127.0.0.1:5174
```

## 指定浏览器

默认使用 Chrome：

```bash
mvn test -Dbrowser=chrome
```

使用 Edge：

```bash
mvn test -Dbrowser=edge
```

IE 浏览器已经停止维护，实际实验中可使用 Edge 的 IE 模式做人工兼容性测试；自动化脚本建议优先在 Chrome 和 Edge 中执行。

## 运行单个测试类

只运行药品查询：

```bash
mvn test -Dtest=MedicineQueryTest
```

只运行 AI 药品推荐：

```bash
mvn test -Dtest=AiMedicineTest
```

只运行费用查询：

```bash
mvn test -Dtest=BillingQueryTest
```

只运行医疗报告：

```bash
mvn test -Dtest=MedicalReportTest
```

## Eclipse 中运行

1. 打开 Eclipse。
2. 选择 `File -> Import -> Maven -> Existing Maven Projects`。
3. Root Directory 选择项目下的 `selenium-tests` 目录。
4. 等待 Maven 下载 Selenium 和 JUnit 依赖。
5. 在 `src/test/java` 中右键测试类。
6. 选择 `Run As -> JUnit Test`。

## 脚本开发可能遇到的问题

| 问题 | 原因 | 处理方式 |
|---|---|---|
| 运行停在 `Running ... AiMedicineTest` | Selenium 正在启动浏览器、下载/查找驱动，或等待前端页面加载 | 先确认浏览器窗口是否已打开；确认 `http://127.0.0.1:5173/medicine` 能手动访问 |
| 出现 `SLF4J(W): No SLF4J providers were found` | Selenium 日志依赖没有绑定具体日志实现 | 这是警告，不影响测试，可忽略 |
| 浏览器无法启动 | Chrome/Edge 版本或 Selenium Manager 下载驱动失败 | 更新浏览器，保证网络可用，或手动配置 WebDriver |
| 登录失败 | 测试账号不存在或密码错误 | 修改 `his.patient.account` 和 `his.patient.password` 参数 |
| 费用页/报告页跳回登录 | Token 未写入或登录接口失败 | 先人工登录确认账号可用，再运行脚本 |
| AI 用例执行时间较长 | AI 异步任务需要等待后端和模型返回 | 自动化脚本只验证提交动作，不强依赖 AI 最终文本 |
| 页面元素定位失败 | 前端页面文案或结构变化 | 根据页面实际按钮文字或 CSS 类调整定位器 |
| IE 无法执行 Selenium | IE 已停止维护，驱动兼容性差 | 使用 Chrome/Edge 执行自动化，IE/Edge IE 模式做人工测试 |

## 卡住时的快速排查

1. 先手动打开：

```text
http://127.0.0.1:5173/medicine
```

如果打不开，先启动前端：

```bash
cd frontend
npm run dev
```

2. 单独运行最简单的药品查询用例：

```bash
cd selenium-tests
mvn test -Dtest=MedicineQueryTest
```

3. 如果浏览器驱动下载很慢或失败，换 Edge 试一次：

```bash
mvn test -Dbrowser=edge -Dtest=MedicineQueryTest
```

4. 如果页面加载慢，可以临时调大等待时间：

```bash
mvn test -Dhis.timeout.seconds=30
```

5. 如果费用或报告测试卡住，先确认患者账号可以在页面上人工登录：

```bash
mvn test -Dtest=BillingQueryTest -Dhis.patient.account=你的手机号 -Dhis.patient.password=你的密码
```

## 测试结果记录建议

报告中建议按以下格式记录：

| 用例编号 | 功能点 | 测试输入 | 预期结果 | 实际结果 | 是否通过 | 缺陷编号 |
|---|---|---|---|---|---|---|
| F09-01 | 药品列表查询 | 无 | 显示药品列表 | 显示药品列表 | 通过 | 无 |
| F10-02 | AI 空输入 | 空字符串 | 发送按钮不可用 | 发送按钮不可用 | 通过 | 无 |
| F11-01 | 未登录访问费用页 | 无 Token | 跳转登录页 | 跳转登录页 | 通过 | 无 |
| F12-04 | 未选择病历生成报告 | 未选择病历 | 生成按钮不可用 | 生成按钮不可用 | 通过 | 无 |

## 作业提交内容

建议提交：

- 测试用例表
- `selenium-tests` 自动化测试脚本
- 测试执行截图
- Maven/JUnit 执行结果截图
- 缺陷记录表
- 完整测试报告
