# 医院信息系统 (HIS) - Postman 测试文档

> 版本：1.0  
> 基础URL：`http://localhost:8080/HIS`  
> 测试前准备：请确保已启动MySQL数据库并执行建表SQL

---

## 目录

1. [环境准备](#环境准备)
2. [Postman Collection 导入](#postman-collection-导入)
3. [全局配置](#全局配置)
4. [测试顺序](#测试顺序)
5. [接口测试详情](#接口测试详情)
   - [患者账户管理](#患者账户管理)
   - [患者信息管理](#患者信息管理)
   - [预约系统](#预约系统)
   - [药品信息查询](#药品信息查询)
   - [医疗费用查询](#医疗费用查询)
   - [医疗报告管理](#医疗报告管理)
   - [科室信息查询](#科室信息查询)
6. [常见问题](#常见问题)

---

## 环境准备

### 1. 启动应用

确保以下服务已启动：
- ✅ MySQL数据库（数据库：`his`）
- ✅ Redis服务（端口：6379）
- ✅ Spring Boot应用（端口：8080）

### 2. 初始化测试数据

在MySQL中执行以下SQL插入测试数据：

```sql
-- 插入科室
INSERT INTO department (name, description, location, status) VALUES
('神经内科', '诊治头痛、头晕、脑血管病等神经系统疾病', '门诊楼3楼', 1),
('骨科', '诊治骨折、关节疾病、脊柱疾病等', '门诊楼2楼', 1),
('心内科', '诊治高血压、冠心病、心律失常等', '门诊楼4楼', 1);

-- 插入医生
INSERT INTO doctor (name, gender, age, title, department_id, specialty, phone, status) VALUES
('王明', '男', 45, '主任医师', 1, '头痛头晕、脑血管病、帕金森病', '13800000001', 1),
('张伟', '男', 50, '主任医师', 2, '关节置换、脊柱外科', '13800000002', 1),
('刘芳', '女', 42, '副主任医师', 3, '高血压、冠心病', '13800000003', 1);

-- 插入药品
INSERT INTO medicine (name, generic_name, category, specification, manufacturer, ingredients, efficacy, side_effects, contraindications, price, stock, status) VALUES
('布洛芬缓释胶囊', '布洛芬', '化学药', '0.3g*20粒', '中美史克', '布洛芬', '用于缓解轻至中度疼痛如头痛、关节痛', '胃肠道不适、恶心、呕吐', '孕妇禁用、消化道溃疡患者禁用', 25.00, 500, 1),
('阿莫西林胶囊', '阿莫西林', '化学药', '0.5g*24粒', '联邦制药', '阿莫西林三水合物', '用于敏感菌所致的呼吸道感染', '恶心、呕吐、腹泻、皮疹', '青霉素过敏者禁用', 15.00, 300, 1);

-- 插入医生排班
INSERT INTO doctor_schedule (doctor_id, schedule_date, time_slot, max_patients, booked_count, status) VALUES
(1, CURDATE(), '08:00-09:00', 20, 0, 1),
(1, CURDATE(), '09:00-10:00', 20, 0, 1),
(2, CURDATE(), '08:00-09:00', 15, 0, 1);
```

---

## Postman Collection 导入

### 创建 Collection

1. 打开Postman
2. 点击左侧 `Collections` → `+` 创建新Collection
3. 命名为：`医院信息系统(HIS)测试`
4. 点击 `Variables` 标签，添加以下变量：

| 变量名 | 初始值 | 说明 |
|--------|--------|------|
| `baseUrl` | `http://localhost:8080/HIS` | 基础URL |
| `phone` | `13900139000` | 测试手机号 |
| `username` | `testuser` | 测试用户名 |
| `password` | `123456` | 测试密码 |

### 创建 Folders

在Collection下创建以下Folder：
- `1.患者账户管理`
- `2.患者信息管理`
- `3.预约系统`
- `4.药品信息查询`
- `5.医疗费用查询`
- `6.医疗报告管理`
- `7.科室信息查询`

---

## 全局配置

### Cookie/Session 配置

由于系统使用Session认证，需要在Postman中启用Cookie管理：

1. 点击右上角设置图标 ⚙️
2. 找到 `Cookies` → `Manage Cookies`
3. 添加域名：`localhost`
4. 确保Cookie自动保存已开启

### 设置 Authorization

登录后，后续请求会自动携带Session Cookie，无需手动设置Token。

---

## 测试顺序

**重要：请按以下顺序测试，因为后续接口需要先登录获取Session**

```
1. 患者注册
2. 患者登录 ← 获取Session
3. 查看个人信息
4. 查询科室列表
5. 查询可用排班
6. 创建预约
7. 查询预约列表
8. 查询药品列表
9. AI药品推荐
10. 创建电子病历（通过数据库直接插入）
11. 生成医疗报告
12. 查询费用列表
13. AI费用解释
14. 查询病历列表
15. 登出
```

---

## 接口测试详情

## 1. 患者账户管理

### 1.1 用户注册

**请求信息**
```
POST {{baseUrl}}/patient/register
Content-Type: application/json
```

**请求体**
```json
{
  "name": "张三",
  "username": "zhangsan",
  "password": "123456",
  "confirmPassword": "123456",
  "gender": "男",
  "age": 25,
  "phone": "13900139000",
  "address": "北京市朝阳区",
  "idCard": "110101199001011234"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "account": "a1b2c3d4-e5f6-...",
    "username": "zhangsan",
    "name": "张三",
    "phone": "13900139000"
  },
  "total": null
}
```

**测试要点**
- ✅ 手机号格式正确（11位，1开头）
- ✅ 身份证格式正确（18位）
- ✅ 两次密码一致
- ❌ 测试错误手机号：`phone: "123"` → 应返回"手机号格式不正确"
- ❌ 测试重复手机号：再次注册同一手机号 → 应返回"该手机号或身份证已被注册"

---

### 1.2 用户登录

**请求信息**
```
POST {{baseUrl}}/patient/login
Content-Type: application/json
```

**请求体**
```json
{
  "phone": "13900139000",
  "password": "123456"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "account": "a1b2c3d4-e5f6-...",
    "username": "zhangsan",
    "name": "张三",
    "gender": "男",
    "age": 25,
    "phone": "13900139000",
    "address": "北京市朝阳区",
    "idCard": "110101199001011234"
  },
  "total": null
}
```

**测试要点**
- ✅ 登录成功后，检查Postman的Cookies中是否包含 `JSESSIONID`
- ❌ 测试错误手机号 → 应返回"手机号不存在"
- ❌ 测试错误密码 → 应返回"密码不正确"

---

### 1.3 查看个人信息

**请求信息**
```
GET {{baseUrl}}/patient/me
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "account": "a1b2c3d4-e5f6-...",
    "username": "zhangsan",
    "name": "张三",
    "gender": "男",
    "age": 25,
    "phone": "13900139000",
    "address": "北京市朝阳区",
    "idCard": "110101199001011234"
  },
  "total": null
}
```

**测试要点**
- ✅ 需要先登录，否则返回401错误
- ✅ 返回数据与登录时一致

---

### 1.4 修改个人信息

**请求信息**
```
PUT {{baseUrl}}/patient/me/update
Content-Type: application/json
```

**请求体**
```json
{
  "username": "zhangsan2",
  "address": "上海市浦东新区",
  "password": "123456"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": "修改成功",
  "total": null
}
```

**测试要点**
- ✅ password为当前密码，用于验证身份
- ✅ 只修改非空字段（address修改为上海市浦东新区）
- ❌ 测试错误当前密码 → 应返回"密码不正确"

---

### 1.5 忘记密码

**请求信息**
```
PUT {{baseUrl}}/patient/login/forget
Content-Type: application/json
```

**请求体**
```json
{
  "phone": "13900139000",
  "newPassword": "654321",
  "confirmPassword": "654321"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": "密码修改成功",
  "total": null
}
```

**测试要点**
- ✅ 使用新密码重新登录验证
- ❌ 测试密码不一致 → 应返回"两次密码不一致"

---

### 1.6 用户登出

**请求信息**
```
POST {{baseUrl}}/patient/loginout
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": "登出成功",
  "total": null
}
```

**测试要点**
- ✅ 登出后再访问 `/patient/me` → 应返回401错误

---

## 2. 患者信息管理

### 2.1 获取当前患者基本信息

**请求信息**
```
GET {{baseUrl}}/patient/info
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "account": "a1b2c3d4-e5f6-...",
    "username": "zhangsan",
    "name": "张三",
    "gender": "男",
    "age": 25,
    "phone": "13900139000",
    "address": "上海市浦东新区",
    "idCard": "110101199001011234",
    "totalVisits": 0,
    "lastVisitDate": null
  },
  "total": null
}
```

**测试要点**
- ✅ totalVisits显示就诊次数（初始为0）
- ✅ lastVisitDate显示最近就诊时间（初始为null）

---

### 2.2 分页查询患者列表

**请求信息**
```
GET {{baseUrl}}/patient/list?keyword=张&page=1&size=10
```

**URL参数**
- `keyword`（可选）：按姓名/手机号/身份证模糊搜索
- `page`（默认1）：页码
- `size`（默认10）：每页数量

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "account": "a1b2c3d4-e5f6-...",
      "username": "zhangsan",
      "name": "张三",
      "phone": "13900139000",
      "gender": "男",
      "age": 25
    }
  ],
  "total": 1
}
```

**测试要点**
- ✅ 关键词搜索：`keyword=139` → 搜索手机号
- ✅ 不传keyword：返回所有患者

---

### 2.3 获取当前患者病历列表

**请求信息**
```
GET {{baseUrl}}/patient/medical-records?page=1&size=10
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [],
  "total": 0
}
```

**测试要点**
- ✅ 初始无病历记录，返回空数组
- ✅ 插入测试数据后再次查询

---

### 2.4 获取病历详情

**请求信息**
```
GET {{baseUrl}}/patient/medical-record/1
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "record": {
      "id": 1,
      "patientName": "张三",
      "doctorName": "王明",
      "departmentName": "神经内科",
      "chiefComplaint": "头痛3天",
      "diagnosis": "偏头痛",
      "visitDate": "2024-04-14T10:00:00"
    },
    "prescription": {
      "id": 1,
      "prescriptionDate": "2024-04-14T10:05:00",
      "status": 1
    },
    "prescriptionItems": [
      {
        "id": 1,
        "medicineName": "布洛芬缓释胶囊",
        "dosage": "每次1粒，每日2次",
        "quantity": 20,
        "days": 10
      }
    ]
  },
  "total": null
}
```

**测试要点**
- ⚠️ 需要先在数据库插入测试数据（病历、处方、处方明细）

---

### 2.5 历史就诊记录

**请求信息**
```
GET {{baseUrl}}/patient/visit-history?startDate=2024-01-01&endDate=2024-12-31&page=1&size=10
```

**URL参数**
- `departmentId`（可选）：按科室筛选
- `doctorId`（可选）：按医生筛选
- `startDate`（可选）：开始日期，格式yyyy-MM-dd
- `endDate`（可选）：结束日期，格式yyyy-MM-dd
- `page` / `size`：分页参数

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "recordId": 1,
      "visitDate": "2024-04-14T10:00:00",
      "doctorName": "王明",
      "departmentName": "神经内科",
      "diagnosis": "偏头痛",
      "prescriptionSummary": "布洛芬缓释胶囊,阿莫西林胶囊"
    }
  ],
  "total": 1
}
```

---

## 3. 预约系统

### 3.1 查询科室列表

**请求信息**
```
GET {{baseUrl}}/department/list
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "name": "神经内科",
      "description": "诊治头痛、头晕、脑血管病等神经系统疾病",
      "location": "门诊楼3楼",
      "status": 1
    },
    {
      "id": 2,
      "name": "骨科",
      "description": "诊治骨折、关节疾病、脊柱疾病等",
      "location": "门诊楼2楼",
      "status": 1
    },
    {
      "id": 3,
      "name": "心内科",
      "description": "诊治高血压、冠心病、心律失常等",
      "location": "门诊楼4楼",
      "status": 1
    }
  ],
  "total": null
}
```

---

### 3.2 查询科室下医生列表

**请求信息**
```
GET {{baseUrl}}/department/1/doctors
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "name": "王明",
      "gender": "男",
      "age": 45,
      "title": "主任医师",
      "departmentId": 1,
      "specialty": "头痛头晕、脑血管病、帕金森病",
      "phone": "13800000001",
      "status": 1
    }
  ],
  "total": null
}
```

---

### 3.3 查询可用排班

**请求信息**
```
GET {{baseUrl}}/appointment/schedules?departmentId=1&date=2024-04-14
```

**URL参数**
- `departmentId`（可选）：科室ID
- `doctorId`（可选）：医生ID
- `date`（可选）：日期，默认查未来7天

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "doctorId": 1,
      "scheduleDate": "2024-04-14",
      "timeSlot": "08:00-09:00",
      "maxPatients": 20,
      "bookedCount": 0,
      "status": 1,
      "doctorName": "王明",
      "departmentName": "神经内科"
    },
    {
      "id": 2,
      "doctorId": 1,
      "scheduleDate": "2024-04-14",
      "timeSlot": "09:00-10:00",
      "maxPatients": 20,
      "bookedCount": 0,
      "status": 1,
      "doctorName": "王明",
      "departmentName": "神经内科"
    }
  ],
  "total": null
}
```

**测试要点**
- ✅ bookedCount < maxPatients 表示可预约
- ✅ status=1 表示可预约

---

### 3.4 创建预约

**请求信息**
```
POST {{baseUrl}}/appointment
Content-Type: application/json
```

**请求体**
```json
{
  "departmentId": 1,
  "doctorId": 1,
  "appointmentType": "DOCTOR",
  "examinationType": null,
  "appointmentDate": "2024-04-14",
  "timeSlot": "09:00-10:00"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "id": 1,
    "patientId": "a1b2c3d4-e5f6-...",
    "doctorId": 1,
    "departmentId": 1,
    "appointmentType": "DOCTOR",
    "appointmentDate": "2024-04-14",
    "timeSlot": "09:00-10:00",
    "status": 0,
    "aiRecommended": 0,
    "patientName": "张三",
    "doctorName": "王明",
    "departmentName": "神经内科"
  },
  "total": null
}
```

**测试要点**
- ✅ status=0 表示已预约
- ❌ 测试无排班时段 → 应返回"该时段无排班"
- ❌ 测试号源已满 → 应返回"该时段已约满"

---

### 3.5 查询预约列表

**请求信息**
```
GET {{baseUrl}}/appointment/list?status=0&page=1&size=10
```

**URL参数**
- `status`（可选）：0=已预约, 1=已完成, 2=已取消
- `departmentId`（可选）
- `doctorId`（可选）
- `startDate` / `endDate`（可选）
- `page` / `size`：分页参数

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "patientId": "a1b2c3d4-e5f6-...",
      "doctorId": 1,
      "departmentId": 1,
      "appointmentType": "DOCTOR",
      "appointmentDate": "2024-04-14",
      "timeSlot": "09:00-10:00",
      "status": 0,
      "patientName": "张三",
      "doctorName": "王明",
      "departmentName": "神经内科"
    }
  ],
  "total": 1
}
```

---

### 3.6 取消预约

**请求信息**
```
PUT {{baseUrl}}/appointment/1/cancel?cancelReason=临时有事
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": "取消成功",
  "total": null
}
```

**测试要点**
- ✅ 取消后预约status变为2
- ❌ 测试取消他人预约 → 应返回"无权操作"
- ❌ 测试取消已取消的预约 → 应返回错误提示

---

### 3.7 改期

**请求信息**
```
PUT {{baseUrl}}/appointment/1/reschedule
Content-Type: application/json
```

**请求体**
```json
{
  "departmentId": 1,
  "doctorId": 1,
  "appointmentType": "DOCTOR",
  "appointmentDate": "2024-04-15",
  "timeSlot": "08:00-09:00"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": "改期成功",
  "total": null
}
```

---

### 3.8 AI智能预约推荐

**请求信息**
```
POST {{baseUrl}}/appointment/ai-recommend
Content-Type: application/json
```

**请求体**
```json
{
  "symptom": "我最近头痛，应该挂什么科？"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "department": "神经内科",
    "doctor": "王明",
    "recommendedTime": "上午09:00-10:00",
    "reason": "头痛症状建议就诊神经内科，王明主任医师擅长头痛头晕等神经系统疾病的诊治"
  },
  "total": null
}
```

**测试要点**
- ⚠️ 需要配置有效的通义千问API Key（`ai.qwen.api-key`）
- ✅ AI返回推荐的科室和医生

---

## 4. 药品信息查询

### 4.1 药品列表

**请求信息**
```
GET {{baseUrl}}/medicine/list?keyword=布洛芬&category=化学药&page=1&size=10
```

**URL参数**
- `keyword`（可选）：按名称/通用名/功效模糊搜索
- `category`（可选）：中成药/化学药/生物制品
- `page` / `size`：分页参数

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "name": "布洛芬缓释胶囊",
      "genericName": "布洛芬",
      "category": "化学药",
      "specification": "0.3g*20粒",
      "manufacturer": "中美史克",
      "price": 25.00,
      "stock": 500,
      "status": 1
    }
  ],
  "total": 1
}
```

---

### 4.2 药品详情

**请求信息**
```
GET {{baseUrl}}/medicine/1
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "id": 1,
    "name": "布洛芬缓释胶囊",
    "genericName": "布洛芬",
    "category": "化学药",
    "specification": "0.3g*20粒",
    "manufacturer": "中美史克",
    "ingredients": "布洛芬",
    "efficacy": "用于缓解轻至中度疼痛如头痛、关节痛、偏头痛、牙痛、肌肉痛、神经痛、痛经，也用于普通感冒或流行性感冒引起的发热",
    "sideEffects": "胃肠道不适、恶心、呕吐，长期使用可致胃溃疡",
    "contraindications": "孕妇禁用、对阿司匹林过敏者禁用、消化道溃疡患者禁用",
    "price": 25.00,
    "stock": 500,
    "status": 1
  },
  "total": null
}
```

---

### 4.3 AI药品推荐

**请求信息**
```
POST {{baseUrl}}/medicine/ai-recommend
Content-Type: application/json
```

**请求体**
```json
{
  "symptom": "头痛发热"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "medicineName": "布洛芬缓释胶囊",
      "reason": "布洛芬具有解热镇痛作用，适用于头痛发热症状",
      "dosage": "成人每次1粒，每日2次，饭后服用",
      "precautions": "孕妇禁用，消化道溃疡患者慎用。AI推荐仅供参考，请遵医嘱"
    }
  ],
  "total": null
}
```

**测试要点**
- ⚠️ 需要配置有效的通义千问API Key
- ✅ 返回推荐的药品列表（最多3种）

---

## 5. 医疗费用查询

### 5.1 费用列表

**请求信息**
```
GET {{baseUrl}}/billing/list?itemType=REGISTRATION&status=0&page=1&size=10
```

**URL参数**
- `itemType`（可选）：REGISTRATION/EXAMINATION/MEDICINE/OTHER
- `status`（可选）：0=未支付, 1=已支付, 2=已退款
- `startDate` / `endDate`（可选）
- `page` / `size`：分页参数

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "patientId": "a1b2c3d4-e5f6-...",
      "appointmentId": 1,
      "medicalRecordId": null,
      "itemType": "REGISTRATION",
      "itemName": "挂号费",
      "amount": 50.00,
      "description": "神经内科门诊挂号",
      "status": 0,
      "createTime": "2024-04-14T09:00:00",
      "patientName": "张三"
    }
  ],
  "total": 1
}
```

---

### 5.2 费用详情

**请求信息**
```
GET {{baseUrl}}/billing/detail/1
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "id": 1,
    "patientId": "a1b2c3d4-e5f6-...",
    "appointmentId": 1,
    "itemType": "REGISTRATION",
    "itemName": "挂号费",
    "amount": 50.00,
    "description": "神经内科门诊挂号",
    "status": 0,
    "createTime": "2024-04-14T09:00:00",
    "patientName": "张三"
  },
  "total": null
}
```

---

### 5.3 AI费用解释

**请求信息**
```
POST {{baseUrl}}/billing/ai-explain
Content-Type: application/json
```

**请求体**
```json
{
  "question": "为什么这么贵？",
  "startDate": "2024-01-01",
  "endDate": "2024-12-31"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "totalAmount": 1580.00,
    "breakdown": "挂号费: 50元(3.2%) | CT检查: 800元(50.6%) | 药品费: 730元(46.2%)",
    "explanation": "您的费用主要由CT检查费构成，这是头部CT的标准检查价格...",
    "suggestion": "建议使用医保结算，部分检查和药品可享受医保报销"
  },
  "total": null
}
```

**测试要点**
- ⚠️ 需要配置有效的通义千问API Key
- ⚠️ 需要先有费用记录（在billing表中插入测试数据）

---

## 6. 医疗报告管理

### 6.1 生成医疗报告

**请求信息**
```
POST {{baseUrl}}/report/generate
Content-Type: application/json
```

**请求体**
```json
{
  "medicalRecordId": 1,
  "reportType": "EXAMINATION",
  "title": "头部检查报告",
  "examinationData": "头部CT: 未见明显异常; 血常规: 白细胞计数偏高 12.5×10⁹/L"
}
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "id": 1,
    "patientId": "a1b2c3d4-e5f6-...",
    "medicalRecordId": 1,
    "doctorId": 1,
    "reportType": "EXAMINATION",
    "title": "头部检查报告",
    "examinationData": "头部CT: 未见明显异常; 血常规: 白细胞计数偏高 12.5×10⁹/L",
    "aiSummary": "患者主诉头痛3天，血常规示白细胞偏高...",
    "aiDiagnosis": "1. 偏头痛(确诊) 2. 上呼吸道感染(疑似，白细胞升高提示)",
    "aiTreatment": "1. 布洛芬缓释胶囊 镇痛治疗 2. 建议进一步检查感染指标",
    "aiRecommendation": "1. 避免过度劳累和精神紧张 2. 一周后复查血常规",
    "pdfPath": null,
    "status": 0,
    "patientName": "张三",
    "doctorName": "王明"
  },
  "total": null
}
```

**测试要点**
- ⚠️ 需要先有病历记录（在medical_record表中插入测试数据）
- ⚠️ 需要配置有效的通义千问API Key
- ✅ status=0 表示草稿状态

---

### 6.2 报告列表

**请求信息**
```
GET {{baseUrl}}/report/list?reportType=EXAMINATION&page=1&size=10
```

**URL参数**
- `reportType`（可选）：EXAMINATION / TREATMENT
- `page` / `size`：分页参数

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "patientId": "a1b2c3d4-e5f6-...",
      "medicalRecordId": 1,
      "doctorId": 1,
      "reportType": "EXAMINATION",
      "title": "头部检查报告",
      "status": 0,
      "createTime": "2024-04-14T11:00:00",
      "patientName": "张三",
      "doctorName": "王明"
    }
  ],
  "total": 1
}
```

---

### 6.3 报告详情

**请求信息**
```
GET {{baseUrl}}/report/1
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "id": 1,
    "patientId": "a1b2c3d4-e5f6-...",
    "medicalRecordId": 1,
    "doctorId": 1,
    "reportType": "EXAMINATION",
    "title": "头部检查报告",
    "examinationData": "头部CT: 未见明显异常; 血常规: 白细胞计数偏高 12.5×10⁹/L",
    "aiSummary": "患者主诉头痛3天，血常规示白细胞偏高...",
    "aiDiagnosis": "1. 偏头痛(确诊) 2. 上呼吸道感染(疑似)",
    "aiTreatment": "1. 布洛芬缓释胶囊 镇痛治疗 2. 建议进一步检查感染指标",
    "aiRecommendation": "1. 避免过度劳累和精神紧张 2. 一周后复查血常规",
    "pdfPath": null,
    "status": 0,
    "patientName": "张三",
    "doctorName": "王明"
  },
  "total": null
}
```

---

### 6.4 导出PDF

**请求信息**
```
GET {{baseUrl}}/report/1/export-pdf
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": "reports/pdf/medical_report_1_20240414110000.pdf",
  "total": null
}
```

**测试要点**
- ✅ 返回PDF文件路径
- ✅ 检查项目目录下是否生成PDF文件

---

### 6.5 确认报告

**请求信息**
```
PUT {{baseUrl}}/report/1/confirm
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": "确认成功",
  "total": null
}
```

**测试要点**
- ✅ 确认后status从0变为1
- ✅ 重复确认仍然返回成功（幂等性）

---

## 7. 科室信息查询

### 7.1 科室列表

**请求信息**
```
GET {{baseUrl}}/department/list
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "name": "神经内科",
      "description": "诊治头痛、头晕、脑血管病等神经系统疾病",
      "location": "门诊楼3楼",
      "status": 1
    },
    {
      "id": 2,
      "name": "骨科",
      "description": "诊治骨折、关节疾病、脊柱疾病等",
      "location": "门诊楼2楼",
      "status": 1
    },
    {
      "id": 3,
      "name": "心内科",
      "description": "诊治高血压、冠心病、心律失常等",
      "location": "门诊楼4楼",
      "status": 1
    }
  ],
  "total": null
}
```

---

### 7.2 科室详情

**请求信息**
```
GET {{baseUrl}}/department/1
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": {
    "id": 1,
    "name": "神经内科",
    "description": "诊治头痛、头晕、脑血管病等神经系统疾病",
    "location": "门诊楼3楼",
    "status": 1,
    "createTime": "2024-04-14T00:00:00",
    "updateTime": "2024-04-14T00:00:00"
  },
  "total": null
}
```

---

### 7.3 科室下医生列表

**请求信息**
```
GET {{baseUrl}}/department/1/doctors
```

**预期响应**
```json
{
  "success": true,
  "errorMsg": null,
  "data": [
    {
      "id": 1,
      "name": "王明",
      "gender": "男",
      "age": 45,
      "title": "主任医师",
      "departmentId": 1,
      "specialty": "头痛头晕、脑血管病、帕金森病",
      "phone": "13800000001",
      "status": 1,
      "createTime": "2024-04-14T00:00:00",
      "updateTime": "2024-04-14T00:00:00"
    }
  ],
  "total": null
}
```

---

## 常见问题

### 1. 401 Unauthorized 错误

**原因：** 未登录或Session已过期

**解决方法：**
- 先调用 `/patient/login` 接口登录
- 检查Postman是否自动保存了Cookie
- 重新登录获取新的Session

---

### 2. AI接口调用失败

**原因：** 未配置通义千问API Key或Key无效

**解决方法：**
- 在 `application.yaml` 中配置有效的 `ai.qwen.api-key`
- 重启应用
- 确保网络可以访问通义千问API

---

### 3. 数据库连接错误

**原因：** MySQL未启动或连接信息错误

**解决方法：**
- 检查MySQL服务是否启动
- 检查 `application.yaml` 中的数据库连接配置
- 确保数据库 `his` 已创建并执行了建表SQL

---

### 4. 测试数据不存在

**原因：** 数据库中缺少必要的测试数据

**解决方法：**
- 执行本文档"环境准备"部分的SQL插入测试数据
- 确保至少有科室、医生、药品、排班等基础数据

---

### 5. 预约失败 - 该时段无排班

**原因：** 请求的日期和时间段在doctor_schedule表中不存在

**解决方法：**
- 先查询可用排班：`GET /appointment/schedules`
- 根据返回的排班数据创建预约

---

### 6. PDF导出失败

**原因：** 报告不存在或文件系统权限问题

**解决方法：**
- 确保报告ID存在
- 检查项目目录下 `reports/pdf/` 目录是否存在且可写

---

## 测试总结

### 核心测试流程

```
1. 注册账号 → 2. 登录 → 3. 查询科室 → 4. 查询排班 → 5. 创建预约
   ↓
6. 查询药品 → 7. 创建病历 → 8. 生成报告 → 9. 导出PDF → 10. 确认报告
```

### 测试覆盖率

| 模块 | 接口数量 | 测试重点 |
|------|----------|----------|
| 患者账户管理 | 6 | 登录认证、密码验证、Session管理 |
| 患者信息管理 | 5 | 病历查询、分页、数据关联 |
| 预约系统 | 7 | 号源管理、取消/改期、AI推荐 |
| 药品信息查询 | 3 | 搜索、详情、AI推荐 |
| 医疗费用查询 | 3 | 费用筛选、AI解释 |
| 医疗报告管理 | 5 | AI生成、PDF导出、确认流程 |
| 科室信息查询 | 3 | 基础查询、数据关联 |

**总计：32个接口**

---

## 附录：Postman Collection JSON

可以将以下JSON导入到Postman中，快速创建完整的测试Collection：

```json
{
  "info": {
    "name": "医院信息系统(HIS)测试",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "variable": [
    {
      "key": "baseUrl",
      "value": "http://localhost:8080/HIS",
      "type": "string"
    },
    {
      "key": "phone",
      "value": "13900139000",
      "type": "string"
    },
    {
      "key": "password",
      "value": "123456",
      "type": "string"
    }
  ],
  "item": [
    {
      "name": "1.患者账户管理",
      "item": [
        {
          "name": "用户注册",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"name\": \"张三\",\n  \"username\": \"zhangsan\",\n  \"password\": \"123456\",\n  \"confirmPassword\": \"123456\",\n  \"gender\": \"男\",\n  \"age\": 25,\n  \"phone\": \"13900139000\",\n  \"address\": \"北京市朝阳区\",\n  \"idCard\": \"110101199001011234\"\n}"
            },
            "url": "{{baseUrl}}/patient/register"
          }
        },
        {
          "name": "用户登录",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"phone\": \"{{phone}}\",\n  \"password\": \"{{password}}\"\n}"
            },
            "url": "{{baseUrl}}/patient/login"
          }
        }
      ]
    }
  ]
}
```

---

**文档版本：v1.0**  
**最后更新：2024-04-14**
