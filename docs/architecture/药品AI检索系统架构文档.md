# 药品 AI 检索系统架构文档

## 一、系统总体架构

```
┌────────────────────────────────────────────────────────────────────────┐
│                    前端 (Vue3 + Element Plus)                           │
│  MedicineView.vue ──> medicine.ts API ──> chatHistory.ts               │
└──────────────┬─────────────────────────────────────────────────────────┘
               │ HTTP
               ▼
┌────────────────────────────────────────────────────────────────────────┐
│                     后端 (Spring Boot)                                  │
│                                                                         │
│  MedicineController                                                     │
│    ├── 同步: /medicine/ai-chat ──> AiMedicineService.medicineChat()    │
│    └── 异步: /medicine/ai-chat-async                                    │
│              ──> AsyncTaskServiceImpl.submitTask()                      │
│                    │                                                    │
│                    ▼  Redis Stream (stream:ai:task)                     │
│                    │                                                    │
│              AsyncTaskConsumer.onMessage()                              │
│                    │                                                    │
│                    ▼                                                    │
│              AiMedicineService.medicineChat()                           │
│                    │                                                    │
│           ┌────────┴────────┐                                          │
│           │  RAG 混合检索     │                                          │
│           │ 向量 + 关键词     │                                          │
│           │  → RRF 融合排序  │                                          │
│           └────────┬────────┘                                          │
│                    ▼                                                    │
│           LLM (Qwen-plus) 生成推荐                                      │
│                    │                                                    │
│                    ▼                                                    │
│           AsyncTaskResult → Redis Hash → 前端轮询                       │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 二、请求流程

### 2.1 异步对话流程（前端实际使用）

```
用户输入消息
  │
  ▼
POST /medicine/ai-chat-async  {message, history}
  │
  ▼
MedicineServiceImpl.aiMedicineChatAsync()
  │  构建 AsyncTaskRequest {taskType="MEDICINE_CHAT", message, historyJson}
  │
  ▼
AsyncTaskServiceImpl.submitTask()
  │  1. 生成16位taskId
  │  2. Redis预存 PENDING 状态 → async:task:result:{taskId} (TTL 10min)
  │  3. 发送到 Redis Stream → stream:ai:task
  │
  ▼ 返回 taskId 给前端
  │
  │ ──────── 前端开始每1.5秒轮询 ────────
  │
  ▼
AsyncTaskConsumer.onMessage()   ← Redis Stream 消费者
  │  1. unwrapRedisValue() 解包双重序列化
  │  2. 更新状态 → PROCESSING
  │  3. 路由到 handleMedicineChat()
  │
  ▼
AiMedicineService.medicineChat(message, history)
  │
  ├─ completed=false → 信息收集中，返回追问
  └─ completed=true  → 触发药品推荐
       │
       ▼
     recommendBySymptom(fullSymptom)
       │
       ▼
     hybridRetrieve(symptom)  ← 核心：混合检索
       │
       ├─ vectorSearch()      向量语义检索
       ├─ keywordSearch()     SQL关键词检索
       └─ rrfFusion()         RRF融合排序
       │
       ▼
     拼装Top-10药品完整上下文 → chatModel.generate() → LLM精排推荐
  │
  ▼
保存 AsyncTaskResult.completed(taskId, resultJson) → Redis Hash
  │
  │ ──────── 前端轮询命中 COMPLETED ────────
  │
  ▼
前端解析 MedicineChatResponse，展示AI回复+推荐药品
```

### 2.2 同步对话流程

```
POST /medicine/ai-chat  {message, history}
  → AiMedicineService.medicineChat()  (同上，但同步等待)
  → 直接返回 MedicineChatResponse
```

---

## 三、分层索引结构

### 3.1 设计思路

传统做法将一个药品的所有信息塞进一个 chunk，导致：
- 向量编码时语义被稀释，功效关键词信号弱
- 超长文本被迫截断，丢失信息
- 无法精确匹配特定维度（如副作用、禁忌）

改进方案：**字段级 Chunk 拆分**，每个药品拆为多个独立 chunk，通过 metadata 关联。

### 3.2 Chunk 分层

| 层级 | 字段标识 | 内容格式 | 作用 |
|------|---------|---------|------|
| **L1 摘要** | `summary` | `药品名: X \| 通用名: X \| 分类: X \| 功效: X` | 快速粗筛，语义聚焦，得分×1.2加权 |
| **L2 功效** | `efficacy` | `药品名: X \| 功效: 详细描述` | 功效维度精确匹配 |
| **L2 成分** | `ingredients` | `药品名: X \| 成分: 详细描述` | 成分维度匹配 |
| **L2 副作用** | `side_effects` | `药品名: X \| 副作用: 详细描述` | 副作用/过敏匹配 |
| **L2 禁忌** | `contraindications` | `药品名: X \| 禁忌: 详细描述` | 禁忌/安全匹配 |
| **L2 用法** | `dosage` | `药品名: X \| 用法用量: X \| 规格: X \| 价格: X` | 用法用量匹配 |

每个 chunk 附加 metadata：
```json
{
  "medicineId": "537",
  "medicineName": "连花清瘟胶囊",
  "field": "summary"
}
```

### 3.3 索引构建流程

```
应用启动
  │
  ├─ medicineEmbeddingStore() Bean 创建
  │    ├─ 存在 medicine-embedding-store.json?
  │    │   ├─ 是 → InMemoryEmbeddingStore.fromFile() 加载 (秒级)
  │    │   │       → embeddingStoreLoadedFromCache = true
  │    │   └─ 否 → new InMemoryEmbeddingStore<>() (空store)
  │    │
  │    ▼
  │  AiMedicineService.run() (CommandLineRunner)
  │    ├─ cacheLoaded? → 跳过
  │    └─ buildIndex()
  │         ├─ 从 MySQL 加载所有 Medicine
  │         ├─ 每个药品 → toTextSegments() → 1~6个chunk
  │         ├─ 分批向量化 (每批25条, DashScope API限制)
  │         │    embeddingModel.embedAll(batch) → 存入 EmbeddingStore
  │         └─ persistToFile() → 序列化到磁盘缓存
  │
  ▼ 服务就绪
```

**数据量参考**：1000+药品 × 约6 chunk ≈ 6000+ chunk，缓存文件约 95MB。

---

## 四、混合检索 RAG 流程

### 4.1 总体流程

```
用户症状: "头痛发烧，青霉素过敏"
  │
  ├──────────────────────┬──────────────────────┐
  │                      │                      │
  ▼                      ▼                      │
向量语义检索          SQL关键词检索              │
vectorSearch()       keywordSearch()            │
  │                      │                      │
  │ 语义相似药品          │ 精确命中药品          │
  │ (模糊匹配能力强)     │ (药名/成分精确命中)   │
  │                      │                      │
  └──────────┬───────────┘                      │
             ▼                                  │
        RRF 融合排序                             │
        rrfFusion()                             │
             │                                  │
             ▼                                  │
      Top-10 候选药品                             │
      从DB加载完整信息                            │
             │                                  │
             ▼                                  │
      拼装上下文 → LLM精排 ◄─────────────────────┘
```

### 4.2 向量语义检索 (`vectorSearch`)

```
1. embeddingModel.embed(symptom) → 查询向量 (调DashScope API)
2. InMemoryEmbeddingStore.search(queryEmbedding, topK=30, minScore=0.4)
3. 按 medicineId 聚合：
   - 同一药品多个chunk命中时，取最高分
   - summary chunk 得分 × 1.2 加权
4. 返回 Top-10 药品及其分数
```

**特点**：捕捉语义相似性，"头痛"能匹配到"解热镇痛"，"感冒"能匹配到"流行性感冒"。

### 4.3 SQL 关键词检索 (`keywordSearch`)

```
1. 分词：按标点/空格切分，过滤2字符以下的关键词
   "头痛发烧，青霉素过敏" → ["头痛", "发烧", "青霉素", "过敏"]
2. 对每个关键词在4个字段中 LIKE 搜索：
   name, generic_name, efficacy, ingredients
3. 字段命中加权打分：
   - name 命中: +3.0 (最精确)
   - generic_name 命中: +2.5
   - efficacy 命中: +2.0
   - ingredients 命中: +1.0
4. 多关键词得分累加，返回 Top-10
```

**特点**：精确匹配药名、成分，用户搜"阿莫西林"直接命中，不会因为语义相似而被稀释。

### 4.4 RRF 融合 (`rrfFusion`)

```
对每个候选药品:
  rrfScore = 0.6 / (K + vectorRank + 1) + 0.4 / (K + keywordRank + 1)

其中:
  K = 60 (平滑常数)
  vectorRank = 向量检索中的排名 (未出现则为∞)
  keywordRank = 关键词检索中的排名 (未出现则为∞)
  0.6 = 向量检索权重 (语义更重要)
  0.4 = 关键词检索权重

按 rrfScore 降序排列 → 取 Top-10
```

**RRF 优势**：不依赖绝对分数，只依赖排名，两种检索的分数尺度不同也不影响融合效果。

### 4.5 上下文拼装

```
Top-10 药品 → medicineMapper.selectBatchIds(ids) → 从DB加载完整信息

每个药品拼装格式:
- [综合评分: 0.0083] 连花清瘟胶囊（通用名: 连花清瘟）| 分类: 中成药 | 规格: 0.35g*24粒 | 价格: 18.00元
  功效: 清瘟解毒、宣肺泄热。用于治疗流行性感冒属热毒袭肺证
  成分: 连翘、金银花、麻黄...
  副作用: 恶心、腹泻、呕吐...
  禁忌: 孕妇、哺乳期妇女、儿童及年老体弱者慎用
  用法用量: 口服。一次4粒，一日3次
```

---

## 五、多轮对话状态机

```
用户: "我头痛" ──→ AI追问: "持续多久了？有过敏吗？" {completed: false}
用户: "两天，青霉素过敏" ──→ AI: "了解了" {completed: true}
                                    │
                                    ▼ 触发推荐
                          buildFullSymptomFromHistory()
                          → "我头痛；两天，青霉素过敏"
                                    │
                                    ▼
                          recommendBySymptom("我头痛；两天，青霉素过敏")
                          → 混合检索 → LLM精排 → 推荐结果
```

**设计要点**：信息收集和药品推荐解耦。AI只负责收集症状，推荐由 RAG 检索 + LLM 精排完成，确保推荐结果来自药品数据库而非 AI 臆造。

---

## 六、缓存与持久化

### 6.1 向量索引缓存

| 项目 | 说明 |
|------|------|
| 存储方式 | `InMemoryEmbeddingStore` 序列化到磁盘 JSON 文件 |
| 文件路径 | `medicine-embedding-store.json` (项目根目录) |
| 文件大小 | ~95MB (1000+药品 × 6 chunk) |
| 加载时机 | 应用启动时 `AiConfig.medicineEmbeddingStore()` Bean 创建 |
| 加载耗时 | 从缓存加载: 秒级；首次构建: 分钟级 (6000+次API调用) |
| 失效策略 | 删除 JSON 文件后重启自动重建 |

### 6.2 Spring Cache 药品数据缓存

| 缓存空间 | Key 示例 | TTL | 说明 |
|----------|---------|-----|------|
| `medicine` | `medicine::list::感冒:中成药:1:10` | 30分钟 | 药品列表查询 |
| `medicine` | `medicine::detail::537` | 30分钟 | 药品详情 |

### 6.3 Redis 异步任务结果

| 项目 | 说明 |
|------|------|
| Key格式 | `async:task:result:{taskId}` |
| TTL | 10分钟自动过期 |
| 状态 | PENDING → PROCESSING → COMPLETED / FAILED |
| 序列化 | GenericJackson2JsonRedisSerializer (带@class类型信息) |

---

## 七、Redis Stream 消息队列

### 7.1 配置

| 项目 | 值 |
|------|-----|
| Stream名称 | `stream:ai:task` |
| 消费者组 | `group:ai:consumer` |
| 消费者命名 | `consumer-{timestamp}` |
| 读取偏移 | `ReadOffset.lastConsumed()` |
| 确认模式 | 自动确认 (`receiveAutoAck`) |

### 7.2 消息格式

```json
{
  "taskId": "6caedc7ea5864983",
  "taskType": "MEDICINE_CHAT",
  "patientId": "",
  "message": "我头痛",
  "startDate": "",
  "endDate": "",
  "symptom": "",
  "reportId": "",
  "historyJson": "[{\"role\":\"user\",\"content\":\"我头痛\"}]"
}
```

### 7.3 任务类型路由

| taskType | 处理方法 | AI服务 |
|----------|---------|--------|
| `BILLING_CHAT` | `handleBillingChat()` | `AiBillingService.billingChat()` |
| `BILLING_EXPLAIN` | `handleBillingExplain()` | `AiBillingService.explainBilling()` |
| `MEDICINE_CHAT` | `handleMedicineChat()` | `AiMedicineService.medicineChat()` |
| `MEDICINE_RECOMMEND` | `handleMedicineRecommend()` | `AiMedicineService.recommendBySymptom()` |

### 7.4 双重序列化问题与修复

`RedisConfig` 使用 `GenericJackson2JsonRedisSerializer` 序列化 value，导致 Stream 中的字符串值被双重序列化：
- `"MEDICINE_CHAT"` → `"\"MEDICINE_CHAT\""` (带引号)
- `[{"role":"user"}]` → `"\"[{\\\"role\\\":\\\"user\\\"}]\""` (双重转义)

**修复**：`AsyncTaskConsumer.onMessage()` 入口处统一 `unwrapRedisValue()` 解包所有值：
```java
Map<String, String> unwrapped = new HashMap<>();
value.forEach((k, v) -> unwrapped.put(k, unwrapRedisValue(v)));
```

---

## 八、API 接口一览

| 方法 | 路径 | 用途 |
|------|------|------|
| GET | `/medicine/list?keyword=&category=&page=1&size=10` | 分页查询药品列表 |
| GET | `/medicine/{id}` | 药品详情 |
| POST | `/medicine/ai-recommend` | 一次性症状推荐 |
| POST | `/medicine/ai-chat` | 多轮对话（同步） |
| POST | `/medicine/ai-chat-async` | 多轮对话（异步） |
| GET | `/medicine/ai-task/{taskId}` | 查询异步任务结果 |

---

## 九、技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue3 + Element Plus + TypeScript |
| 后端 | Spring Boot 3.2.5 + Java 21 |
| ORM | MyBatis-Plus 3.5.5 |
| AI框架 | LangChain4j 0.36.2 |
| LLM | 通义千问 qwen-plus (兼容OpenAI协议) |
| Embedding | text-embedding-v2 (DashScope) |
| 向量存储 | InMemoryEmbeddingStore (磁盘持久化) |
| 缓存 | Spring Cache + Redis (GenericJackson2JsonRedisSerializer) |
| 消息队列 | Redis Stream |
| 数据库 | MySQL 8.x |
