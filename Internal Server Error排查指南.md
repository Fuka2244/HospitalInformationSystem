# Internal Server Error (HTTP 500) 排查指南

## 错误含义
"Internal Server Error" 表示服务器在处理请求时发生了未捕获的异常，导致无法完成请求处理。

## 常见原因

### 1. 数据库连接问题
- **现象**: MySQL 未启动、端口错误、密码错误
- **检查**: `application.yaml` 中的 `spring.datasource` 配置
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/his?...
    username: root
    password: Gg20041213  # 确认密码正确
```

### 2. Redis 连接问题
- **现象**: Redis 未启动、端口错误
- **检查**: `application.yaml` 中的 `spring.data.redis` 配置
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### 3. 空指针异常 (NullPointerException)
- **现象**: 对象为 null 但调用其方法
- **常见位置**: Controller 中直接访问可能为 null 的参数
- **排查**: 检查 Controller 中的路径变量和请求体

### 4. AI API 配置问题
- **现象**: Qwen API Key 无效或网络不通
- **检查**: `application.yaml` 中的 `ai.qwen.api-key` 配置

### 5. 数据库查询失败
- **现象**: SQL 语法错误、表不存在、字段不存在
- **排查**: 查看 MyBatis-Plus 日志

## 排查步骤

### 第一步：启用详细日志
确保 `application.yaml` 中启用了 MyBatis 日志：
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl  # 已启用
```

### 第二步：查看控制台日志
启动应用后，所有异常会打印到控制台，查找：
- `Exception in thread`
- `Caused by:`
- 具体的堆栈信息

### 第三步：使用全局异常处理器
项目已创建 `GlobalExceptionHandler.java`，会捕获所有异常并返回友好错误信息。

## 常见场景及解决方案

### 场景 1：登录返回 500 错误
```
POST /HIS/patient/login
```
**可能原因**：
- 用户名或密码格式错误
- 数据库连接失败
- 密码加密失败

**解决方案**：
1. 检查 MySQL 是否启动
2. 检查 `patient` 表是否存在
3. 确认密码格式符合 `RegexTool.passwordRegex` 正则表达式

### 场景 2：AI 推荐返回 500 错误
```
POST /HIS/appointment/ai-recommend
POST /HIS/medicine/ai-recommend
```
**可能原因**：
- Qwen API Key 无效
- 网络连接失败
- JSON 解析失败

**解决方案**：
1. 替换 `application.yaml` 中的 `YOUR_QWEN_API_KEY` 为有效密钥
2. 检查网络连接
3. 查看 `AiConfig.java` 配置

### 场景 3：数据库查询返回 500 错误
```
GET /HIS/patient/list
```
**可能原因**：
- SQL 语法错误
- 表不存在
- 字段映射错误

**解决方案**：
1. 检查数据库表结构是否与 `entity` 类匹配
2. 查看控制台 SQL 日志
3. 确认 MyBatis-Plus 配置正确

## 快速诊断命令

### 测试数据库连接
```bash
mysql -u root -p -h localhost -P 3306 -e "USE his; SELECT COUNT(*) FROM patient;"
```

### 测试 Redis 连接
```bash
redis-cli ping
# 应返回: PONG
```

### 测试应用启动
```bash
mvn spring-boot:run
```

## 添加日志输出

在怀疑出错的地方添加日志：
```java
log.info("当前参数: {}", param);
log.error("发生错误: {}", e.getMessage(), e);
```

## 联系支持
如果以上步骤都无法解决问题，请：
1. 截取完整的错误堆栈信息
2. 说明访问的接口和参数
3. 提供应用启动日志
