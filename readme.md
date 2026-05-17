# 🏥 医院信息系统 (Hospital Information System)

一个基于 Spring Boot + Vue.js 的现代化医院信息管理系统，集成 AI 智能服务，提供患者管理、预约挂号、药品管理、账单管理等核心功能。

## ✨ 项目特色

- 🤖 **AI 智能服务**：集成 LangChain4j + 通义千问，实现智能药品推荐、账单解释、医疗报告生成
- ⚡ **高性能缓存**：Redis 缓存 + 分布式锁，提升系统响应速度
- 📊 **数据可视化**：直观的科室展示、预约统计、账单分析
- 🔒 **安全可靠**：密码加密、Session 管理、权限控制
- 📱 **响应式设计**：适配桌面和移动设备
- 🎯 **模块化架构**：前后端分离，易于维护和扩展

## 🛠️ 技术栈

### 后端
- **框架**：Spring Boot 3.2.5
- **语言**：Java 21
- **数据库**：MySQL 8.0
- **ORM**：MyBatis Plus 3.5.5
- **缓存**：Redis
- **AI 服务**：LangChain4j 0.36.2 + 通义千问 (Qwen)
- **安全**：Spring Security Crypto
- **文档生成**：iText PDF

### 前端
- **框架**：Vue.js 3
- **UI 组件**：Element Plus
- **状态管理**：Pinia
- **路由**：Vue Router
- **HTTP 客户端**：Axios
- **构建工具**：Vite

## 📁 项目结构

```
HospitalInformationSystem/
├── src/
│   ├── main/
│   │   ├── java/com/hospitalinfo/hospitalinformationsystem/
│   │   │   ├── ai/                          # AI 智能服务
│   │   │   │   ├── AiMedicineService.java   # 药品推荐
│   │   │   │   ├── AiBillingService.java    # 账单解释
│   │   │   │   ├── AiReportService.java     # 报告生成
│   │   │   │   └── AiAppointmentService.java# 预约推荐
│   │   │   ├── config/                      # 配置类
│   │   │   ├── controller/                  # 控制器
│   │   │   ├── dto/                         # 数据传输对象
│   │   │   ├── entity/                      # 实体类
│   │   │   ├── mapper/                      # MyBatis Mapper
│   │   │   ├── service/                     # 业务逻辑
│   │   │   └── utils/                       # 工具类
│   │   └── resources/
│   │       ├── application.yaml             # 应用配置
│   │       └── sql/                         # 数据库脚本
│   └── test/                                # 测试代码
├── frontend/                                # Vue.js 前端
│   ├── dist/                                # 构建产物
│   ├── node_modules/                        # 依赖包
│   └── index.html                           # 入口文件
└── pom.xml                                  # Maven 配置
```

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.6+
- Node.js 16+
- MySQL 8.0+
- Redis 6.0+

### 后端启动

1. **克隆项目**
```bash
git clone https://github.com/yourusername/HospitalInformationSystem.git
cd HospitalInformationSystem
```

2. **配置数据库**
```sql
CREATE DATABASE his CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

3. **执行数据库脚本**
```bash
# 执行 src/main/resources/sql/his_schema.sql 创建表结构
# 执行 src/main/resources/sql/insert_test_data.sql 插入测试数据
```

4. **修改配置文件**
编辑 `src/main/resources/application.yaml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/his?serverTimezone=Asia/Shanghai
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password

ai:
  qwen:
    api-key: your_qwen_api_key
```

5. **启动后端服务**
```bash
mvn spring-boot:run
```

后端服务将在 `http://localhost:8080/HIS` 启动

### 前端启动

1. **安装依赖**
```bash
cd frontend
npm install
```

2. **启动开发服务器**
```bash
npm run dev
```

前端服务将在 `http://localhost:5173` 启动

3. **构建生产版本**
```bash
npm run build
```

## 📋 核心功能

### 患者管理
- 用户注册与登录
- 个人信息管理
- 密码修改
- 头像上传

### 预约挂号
- 科室浏览
- 医生排班查询
- 在线预约
- 预约记录管理
- AI 智能预约推荐

### 药品管理
- 药品信息查询
- AI 智能药品推荐
- 药品详情展示
- 处方管理

### 账单管理
- 账单查询
- 费用明细
- AI 账单解释
- 支付状态跟踪

### 医疗报告
- 报告生成
- PDF 导出
- AI 辅助报告生成
- 报告历史记录

### AI 智能服务
- **药品推荐**：基于症状和病情推荐合适药品
- **账单解释**：智能解释医疗费用构成
- **报告生成**：AI 辅助生成医疗报告
- **预约推荐**：根据患者需求推荐合适的医生和时间

## 🔧 配置说明

### AI 服务配置

系统使用通义千问 API 提供 AI 服务，需要配置以下参数：

```yaml
ai:
  qwen:
    api-key: ${DASH_SCOPE_API_KEY:YOUR_QWEN_API_KEY}
    model-name: qwen-plus
    base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

### Redis 缓存配置

```yaml
spring:
  cache:
    type: redis  # 设置为 none 可关闭缓存
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
```

### 文件上传配置

```yaml
file:
  upload-dir: ${user.dir}/uploads
  avatar-max-size: 5242880  # 5MB
```

## 📊 数据库设计

主要数据表：
- `patient` - 患者信息
- `doctor` - 医生信息
- `department` - 科室信息
- `appointment` - 预约记录
- `medicine` - 药品信息
- `billing` - 账单信息
- `medical_record` - 医疗记录
- `medical_report` - 医疗报告
- `chat_history` - AI 对话历史

## 🧪 测试

项目包含完整的单元测试和集成测试：

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=AiServiceTest

# 运行缓存性能测试
mvn test -Dtest=CachePerformanceTest
```

## 📈 性能优化

- **Redis 缓存**：热点数据缓存，减少数据库查询
- **分布式锁**：防止并发问题
- **向量检索**：AI 药品推荐使用语义搜索
- **异步处理**：耗时任务异步执行
- **连接池**：数据库连接池优化

## 🔐 安全特性

- 密码 BCrypt 加密存储
- Session 会话管理
- 登录拦截器
- SQL 注入防护
- XSS 攻击防护
- CSRF 令牌验证



⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！