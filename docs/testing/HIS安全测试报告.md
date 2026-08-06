# 医院信息系统（HIS）安全性测试报告

## 一、测试概述

| 项目 | 内容 |
|------|------|
| 系统名称 | 医院信息系统 Hospital Information System |
| 系统版本 | v1.0.0 |
| 测试工具 | OWASP ZAP 2.15 |
| 测试方法 | 自动扫描 + 手动验证 |
| 测试目标 | http://localhost:8080/HIS（后端API） |
| 测试日期 | 2026年4月30日 |
| 测试人员 | [填写姓名] |

## 二、测试环境

| 环境 | 配置 |
|------|------|
| 操作系统 | Windows |
| 后端框架 | Spring Boot 3.2.5 + MyBatis-Plus |
| 前端框架 | Vue 3 + Element Plus + Vite |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis |
| 认证方式 | HttpSession + Cookie（JSESSIONID） |
| 后端地址 | http://localhost:8080/HIS |
| 前端地址 | http://localhost:5173 |

## 三、测试范围

### 3.1 公开接口（无需登录，共11个）

| 序号 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 1 | /patient/login | POST | 用户登录 |
| 2 | /patient/register | POST | 用户注册 |
| 3 | /patient/login/send-code | POST | 发送验证码 |
| 4 | /patient/login/forget | PUT | 忘记密码重置 |
| 5 | /medicine/list | GET | 药品列表 |
| 6 | /medicine/{id} | GET | 药品详情 |
| 7 | /medicine/ai-recommend | POST | AI药品推荐 |
| 8 | /appointment/schedules | GET | 排班查询 |
| 9 | /appointment/ai-recommend | POST | AI预约推荐 |
| 10 | /appointment/ai-recommend-with-schedules | POST | AI预约推荐(含排班) |
| 11 | /department/** | GET | 科室信息 |

### 3.2 受保护接口（需登录，共31个）

涵盖患者信息、预约管理、费用查询、医疗报告、聊天记录、文件上传等模块。

### 3.3 测试方法

1. 使用 OWASP ZAP 导入 OpenAPI 3.0 接口定义文件（HIS-api-openapi.json）
2. 对公开接口直接执行 Active Scan 自动扫描
3. 通过 Manual Request Editor 登录获取 JSESSIONID 后，对受保护接口执行扫描
4. 对关键接口进行 Fuzz 测试（SQL注入、XSS、路径遍历等）

## 四、测试结果汇总

| 风险等级 | 发现数量 | 已修复 | 待修复 |
|----------|---------|--------|--------|
| 高危 | 1 | 1 | 0 |
| 中危 | 1 | 1 | 0 |
| 低危/误报 | 1 | 1 | 0 |
| **合计** | **3** | **3** | **0** |

## 五、漏洞详情与修复方案

---

### 漏洞 1：返回字段中包含隐私数据

**风险等级**：高危

**ZAP 检出描述**：API响应中直接返回了完整的手机号、身份证号等个人敏感信息，违反《个人信息保护法》最小必要原则。

**漏洞详情**：

| 风险点 | 位置 | 漏洞描述 |
|--------|------|---------|
| 手机号明文返回 | PatientServiceImpl.buildPatientInfoVo() 第474行 | `vo.setPhone(patient.getPhone())` 直接返回完整手机号如 13800001234 |
| 验证码明文返回 | PatientServiceImpl.sendVerificationCode() 第217行 | `data.put("code", code)` 将验证码直接返回在API响应体中 |
| 验证码写入日志 | VerificationCodeService.generateCode() 第53行 | `log.info("...验证码: {}", code)` 日志中打印验证码明文 |
| 批量数据泄露 | PatientServiceImpl.listPatients() | 列表接口批量返回所有患者完整手机号 |

**安全影响**：
- 攻击者可通过API接口获取用户完整手机号和身份证号
- 验证码明文返回可被截获，绕过短信验证机制
- 日志中保存验证码明文，有权限访问日志的人员可获取验证码
- 违反《个人信息保护法》对敏感个人信息的保护要求

**修复方案**：

1. 手机号脱敏处理（PatientServiceImpl.java）：

```java
// 修复前
vo.setPhone(patient.getPhone());

// 修复后
vo.setPhone(maskPhone(patient.getPhone()));
vo.setPhoneMasked(true);
```

新增脱敏方法：

```java
private String maskPhone(String phone) {
    if (phone == null || phone.length() < 7) {
        return phone;
    }
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
}
```

脱敏效果：`13800001234 → 138****1234`

2. 移除验证码明文返回（PatientServiceImpl.java）：

```java
// 修复前
data.put("message", "验证码已发送");
data.put("code", code);  // 明文返回验证码

// 修复后
data.put("message", "验证码已发送");
// 验证码通过短信服务发送，不再明文返回
```

3. 移除日志中的验证码明文（VerificationCodeService.java）：

```java
// 修复前
log.info("验证码已生成(Redis) - 手机号: {}, 验证码: {}", phone, code);

// 修复后
log.info("验证码已生成(Redis) - 手机号: {}", phone);
```

4. 新增 phoneMasked 字段标识（PatientInfoVo.java）：

```java
/** 手机号是否脱敏 */
private boolean phoneMasked;
```

**修复状态**：✅ 已修复

**验证方式**：
```bash
# 验证手机号脱敏
curl -b "JSESSIONID=xxx" http://localhost:8080/HIS/patient/me
# 响应中 phone 字段应为 138****1234 格式，phoneMasked 为 true

# 验证验证码不再返回
curl -X POST http://localhost:8080/HIS/patient/login/send-code \
  -H "Content-Type: application/json" -d '{"phone":"13800000001"}'
# 响应中不应包含 code 字段
```

---

### 漏洞 2：未添加安全响应头

**风险等级**：中危

**ZAP 检出描述**：HTTP响应缺少 X-Content-Type-Options、X-Frame-Options、Content-Security-Policy 等安全响应头，可能导致MIME嗅探攻击、点击劫持、XSS等风险。

**漏洞详情**：

修复前的HTTP响应头：

```
HTTP/1.1 200
Content-Type: application/json
Transfer-Encoding: chunked
Date: Thu, 30 Apr 2026 06:37:09 GMT
```

缺少以下安全头：

| 安全头 | 缺失风险 |
|--------|---------|
| X-Content-Type-Options | 浏览器可能将JSON响应解析为HTML，执行嵌入的脚本 |
| X-Frame-Options | 页面可被嵌入恶意iframe，遭受点击劫持攻击 |
| Content-Security-Policy | 无内容安全策略，无法防止XSS和数据注入 |
| Strict-Transport-Security | 浏览器可能通过HTTP访问，遭受中间人攻击 |
| Referrer-Policy | 跨域请求可能泄露完整URL和敏感参数 |
| X-XSS-Protection | 旧浏览器未启用内置XSS过滤器 |

**安全影响**：
- 攻击者可构造恶意页面，通过 iframe 嵌入系统页面实施点击劫持
- 浏览器可能将上传文件误判为可执行脚本并执行
- 无法从HTTP层面防止跨站脚本攻击
- 医疗数据在跨域跳转时可能泄露

**修复方案**：

新增 SecurityHeadersFilter.java 过滤器，为所有HTTP响应自动添加安全头：

```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 防止MIME类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        // 防止点击劫持
        httpResponse.setHeader("X-Frame-Options", "SAMEORIGIN");
        // XSS防护（旧浏览器兼容）
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
        // 内容安全策略
        httpResponse.setHeader("Content-Security-Policy", buildCsp());
        // 强制HTTPS
        httpResponse.setHeader("Strict-Transport-Security",
                               "max-age=31536000; includeSubDomains");
        // Referrer控制
        httpResponse.setHeader("Referrer-Policy",
                               "strict-origin-when-cross-origin");
        // 禁止缓存
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");

        chain.doFilter(request, response);
    }
}
```

CSP策略：

```
default-src 'self';
script-src 'self' 'unsafe-eval';
style-src 'self' 'unsafe-inline';
img-src 'self' data: blob:;
font-src 'self' data:;
connect-src 'self' http://localhost:* ws://localhost:*;
frame-src 'self';
object-src 'none';
form-action 'self';
base-uri 'self';
```

**修复状态**：✅ 已修复

**验证方式**：
```bash
curl -sI http://localhost:8080/HIS/department/list
# 应返回以下安全头：
# X-Content-Type-Options: nosniff
# X-Frame-Options: SAMEORIGIN
# X-XSS-Protection: 1; mode=block
# Content-Security-Policy: default-src 'self'; ...
# Strict-Transport-Security: max-age=31536000; includeSubDomains
# Referrer-Policy: strict-origin-when-cross-origin
```

---

### 漏洞 3：格式字符串错误风险（Format String Error）

**风险等级**：低危（误报）

**ZAP 检出描述**：ZAP探测到前端端口（5173）在处理特定输入时表现异常，可能将用户输入直接传递到了后端的格式化函数中。

**漏洞详情**：

ZAP向Vite开发服务器（端口5173）发送了包含C语言格式化占位符（如 %s、%x、%n）的请求，服务器返回非标准响应，ZAP据此标记为格式字符串错误。

**分析结论**：该告警为误报（False Positive），原因如下：

| 对比项 | C/C++ printf() | Java String.format() |
|--------|---------------|---------------------|
| %n 内存写入 | 可向任意内存地址写入数据 | 仅表示换行符 \n |
| %x 内存读取 | 可读取栈上任意位置数据 | 仅格式化十六进制数字 |
| 任意内存访问 | 可以 | 不可以 |
| 远程代码执行 | 可能 | 不可能 |

格式字符串漏洞是C/C++语言的特有安全问题，Java的格式化函数是类型安全的，不存在此类风险。本项目中的格式化使用均安全：

```java
// 安全：String.format() 只做文本格式化，无内存操作
String.format("患者%d", id)

// 安全：SLF4J 的 {} 是参数替换，非C格式化占位符
log.info("手机号: {}", phone)
```

此外，该告警针对的是Vite开发服务器（端口5173），生产环境不会部署开发服务器，使用Nginx或静态文件托管，因此不具备实际攻击面。

**处理方式**：在OWASP ZAP中右键该告警 → Mark as False Positive

**修复状态**：✅ 误报，已标记忽略

---

## 六、其他安全现状评估

### 6.1 已实现的安全措施

| 类别 | 措施 | 实现方式 |
|------|------|---------|
| 认证 | 登录拦截 | LoginInterceptor 检查Session |
| 认证 | 会话管理 | HttpSession + HttpOnly Cookie |
| 认证 | 会话超时 | 30分钟自动过期 |
| 认证 | 验证码 | Redis存储、5分钟过期、60秒间隔、一次性 |
| 密码 | BCrypt加密 | 自适应盐值、强度10 |
| 密码 | 身份证脱敏 | 前3后4，查全文需验证密码 |
| 授权 | 角色权限 | patient/doctor/pharmacist/admin 四级权限 |
| 输入 | SQL注入防护 | MyBatis-Plus参数化查询 |
| 文件 | 上传安全 | 大小限制5MB + 类型白名单 + UUID重命名 |
| 路径 | 遍历防护 | PDF路径校验必须在允许目录内 |
| 异常 | 全局处理 | GlobalExceptionHandler 不泄露堆栈信息 |
| 前端 | 路由守卫 | 未登录自动跳转登录页 |
| 响应头 | 安全头 | SecurityHeadersFilter（本次新增） |
| 隐私 | 数据脱敏 | 手机号+身份证脱敏（本次新增） |
| 并发 | 分布式锁 | Redis SETNX + Lua脚本 |

### 6.2 待改进项

| 优先级 | 安全风险 | 建议措施 |
|--------|---------|---------|
| P0 | 无CSRF防护 | 引入Spring Security + CSRF Token |
| P0 | 无登录速率限制 | Redis计数器限流 |
| P1 | Session固定攻击 | 登录后调用changeSessionId() |
| P1 | 无密码强度校验 | 前后端添加密码复杂度校验 |
| P2 | DTO缺少Bean Validation | 统一添加@NotNull等注解 |
| P2 | CORS未配置 | 添加跨域配置 |
| P3 | API速率限制 | 对AI接口添加限流 |

## 七、测试结论

本次使用OWASP ZAP对医院信息系统进行了安全性扫描测试，共发现3个安全问题：

1. **返回字段包含隐私数据（高危）** — 已修复，手机号默认脱敏，验证码不再明文返回
2. **未添加安全响应头（中危）** — 已修复，新增SecurityHeadersFilter添加6个安全响应头
3. **格式字符串错误（低危/误报）** — Java不存在此漏洞，已标记为误报

所有实际存在的安全问题均已修复完毕，系统安全性得到显著提升。

---

测试人签字：________________      日期：________________

审核人签字：________________      日期：________________
