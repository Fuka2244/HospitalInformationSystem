<template>
  <div class="page medicine-container">
    <div class="page-header">
      <div>
        <div class="page-title">药品查询</div>
        <div class="page-subtitle">搜索药品信息，并通过 AI 获取用药建议（仅供参考）</div>
      </div>
    </div>
    <el-row class="fill-row" :gutter="20">
      <!-- 药品列表 -->
      <el-col :xs="24" :lg="16" class="fill-col">
        <el-card class="fill-card list-card" shadow="hover">
          <template #header>
            <div class="header-row">
              <div class="head-left">
                <span>药品信息</span>
                <el-tag effect="light" type="info" size="small">共 {{ total }} 条</el-tag>
              </div>
              <div class="filters">
                <el-input v-model="queryParams.keyword" placeholder="搜索药品名称/功效" clearable style="width: 220px; margin-right: 8px" @keyup.enter="handleSearch" />
                <el-select v-model="queryParams.category" placeholder="药品分类" clearable style="width: 180px; margin-right: 8px">
                  <el-option label="中成药" value="中成药" />
                  <el-option label="化学药品与生物制品" value="化学药品与生物制品" />
                </el-select>
                <el-button type="primary" @click="handleSearch">搜索</el-button>
              </div>
            </div>
          </template>

          <el-table :data="medicines" v-loading="loading" stripe>
            <el-table-column prop="name" label="药品名称" width="160" />
            <el-table-column prop="genericName" label="通用名" width="120" />
            <el-table-column prop="category" label="分类" width="90">
              <template #default="{ row }">
                <el-tag :type="row.category === '化学药品与生物制品' ? '' : row.category === '中成药' ? 'success' : 'warning'" size="small">
                  {{ row.category }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="specification" label="规格" width="120" />
            <el-table-column prop="efficacy" label="功效" min-width="180" show-overflow-tooltip />
            <el-table-column prop="dosage" label="用法用量" min-width="160" show-overflow-tooltip />
            <el-table-column prop="price" label="价格" width="90">
              <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @change="loadMedicines"
          />
        </el-card>
      </el-col>

      <!-- AI药品推荐对话 -->
      <el-col :xs="24" :lg="8" class="fill-col">
        <el-card class="fill-card ai-card" shadow="hover">
          <template #header>
            <div class="card-head">
              <span>AI 药品推荐</span>
              <div style="display:flex;gap:8px;align-items:center;">
                <el-tag v-if="chatCompleted" effect="light" type="success" size="small">推荐完成</el-tag>
                <el-tag v-else effect="light" type="warning" size="small">仅供参考</el-tag>
                <el-button v-if="chatMessages.length > 1" link type="info" size="small" @click="resetChat">重新开始</el-button>
              </div>
            </div>
          </template>

          <!-- 聊天消息列表 -->
          <div class="chat-messages" ref="chatMessagesRef">
            <div v-for="(msg, idx) in chatMessages" :key="idx" class="chat-message" :class="msg.role">
              <div class="chat-avatar">
                <el-avatar v-if="msg.role === 'assistant'" :size="32" style="background: linear-gradient(135deg, #2f80ed, #7857ff);">
                  AI
                </el-avatar>
                <el-avatar v-else :size="32" style="background: #67c23a;">
                  我
                </el-avatar>
              </div>
              <div class="chat-bubble">
                <div class="chat-content">{{ msg.content }}</div>
              </div>
            </div>
            <!-- AI正在输入提示 -->
            <div v-if="chatLoading" class="chat-message assistant">
              <div class="chat-avatar">
                <el-avatar :size="32" style="background: linear-gradient(135deg, #2f80ed, #7857ff);">AI</el-avatar>
              </div>
              <div class="chat-bubble">
                <div class="chat-typing">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 推荐结果区域 -->
          <div v-if="aiResults.length > 0 && chatCompleted" class="ai-result">
            <el-divider>推荐结果</el-divider>
            <el-alert type="warning" :closable="false" style="margin-bottom: 12px">
              AI推荐仅供参考，请遵医嘱用药
            </el-alert>
            <el-card v-for="(item, idx) in aiResults" :key="idx" class="ai-item-card" shadow="never" style="margin-bottom: 12px">
              <template #header>
                <strong>{{ item.medicineName }}</strong>
              </template>
              <p><strong>推荐理由：</strong>{{ item.reason }}</p>
              <p><strong>用法用量：</strong>{{ item.dosage }}</p>
              <p><strong>注意事项：</strong>{{ item.precautions }}</p>
            </el-card>
          </div>

          <!-- 输入框 -->
          <div class="chat-input-area">
            <el-input
              v-model="chatInput"
              placeholder="请描述您的症状或回答问题..."
              :disabled="chatLoading || chatCompleted"
              @keyup.enter="sendChatMessage"
            >
              <template #append>
                <el-button :loading="chatLoading" :disabled="!chatInput.trim() || chatCompleted" @click="sendChatMessage">
                  发送
                </el-button>
              </template>
            </el-input>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 药品详情对话框 -->
    <el-dialog v-model="detailVisible" title="药品详情" width="600px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="药品名称">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="通用名">{{ detail.genericName }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item>
            <el-descriptions-item label="规格">{{ detail.specification }}</el-descriptions-item>
            <el-descriptions-item label="生产厂家" :span="2">{{ detail.manufacturer }}</el-descriptions-item>
            <el-descriptions-item label="价格">¥{{ detail.price?.toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="库存">{{ detail.stock }}</el-descriptions-item>
            <el-descriptions-item label="成分" :span="2">{{ detail.ingredients }}</el-descriptions-item>
            <el-descriptions-item label="功效" :span="2">{{ detail.efficacy }}</el-descriptions-item>
            <el-descriptions-item label="用法用量" :span="2">{{ detail.dosage }}</el-descriptions-item>
            <el-descriptions-item label="副作用" :span="2">{{ detail.sideEffects }}</el-descriptions-item>
            <el-descriptions-item label="禁忌" :span="2">{{ detail.contraindications }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getMedicineList, getMedicineDetail, aiMedicineChatAsync, getMedicineAiTaskResult } from '@/api/medicine'
import type { Medicine, MedicineListParams, MedicineRecommendation, MedicineChatResponse, ChatMessageDto } from '@/types'

const loading = ref(false)
const medicines = ref<Medicine[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Medicine | null>(null)

// 聊天相关状态
const chatMessages = ref<ChatMessageDto[]>([
  { role: 'assistant', content: '您好！我是AI药学助手，请问您今天哪里不舒服？请描述一下您的症状。' }
])
const chatInput = ref('')
const chatLoading = ref(false)
const chatCompleted = ref(false)
const chatMessagesRef = ref<HTMLElement | null>(null)
const aiResults = ref<MedicineRecommendation[]>([])

const queryParams = reactive<MedicineListParams>({ page: 1, size: 10 })

// 滚动聊天到底部
async function scrollToBottom() {
  await nextTick()
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

// 发送聊天消息（异步提交+轮询结果）
let pollingTimer: ReturnType<typeof setInterval> | null = null

async function sendChatMessage() {
  const msg = chatInput.value.trim()
  if (!msg || chatLoading.value || chatCompleted.value) return

  chatMessages.value.push({ role: 'user', content: msg })
  chatInput.value = ''
  chatLoading.value = true
  scrollToBottom()

  try {
    const history = chatMessages.value.slice(0, -1)

    // 提交异步任务
    const submitRes = await aiMedicineChatAsync({ message: msg, history })
    const taskId = submitRes.data

    // 轮询任务结果
    pollingTimer = setInterval(async () => {
      try {
        const taskRes = await getMedicineAiTaskResult(taskId)
        const task = taskRes.data

        if (task.status === 'COMPLETED') {
          clearInterval(pollingTimer!)
          pollingTimer = null
          chatLoading.value = false

          const chatResponse: MedicineChatResponse = JSON.parse(task.resultJson!)
          chatMessages.value.push({ role: 'assistant', content: chatResponse.reply })
          scrollToBottom()

          if (chatResponse.completed) {
            chatCompleted.value = true
            aiResults.value = chatResponse.recommendations || []
            if (aiResults.value.length === 0) {
              ElMessage.info('暂未找到合适的推荐药品')
            }
          }
        } else if (task.status === 'FAILED') {
          clearInterval(pollingTimer!)
          pollingTimer = null
          chatLoading.value = false
          chatMessages.value.push({ role: 'assistant', content: '抱歉，AI服务处理失败，请稍后再试。' })
          scrollToBottom()
        }
      } catch {
        clearInterval(pollingTimer!)
        pollingTimer = null
        chatLoading.value = false
        chatMessages.value.push({ role: 'assistant', content: '抱歉，获取AI响应失败，请稍后再试。' })
        scrollToBottom()
      }
    }, 1500)
  } catch (error) {
    chatLoading.value = false
    chatMessages.value.push({ role: 'assistant', content: '抱歉，AI药学服务暂时不可用，请稍后重试。' })
    scrollToBottom()
  }
}

// 组件卸载时清除轮询
onUnmounted(() => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
  }
})

// 重置聊天
function resetChat() {
  chatMessages.value = [
    { role: 'assistant', content: '您好！我是AI药学助手，请问您今天哪里不舒服？请描述一下您的症状。' }
  ]
  chatInput.value = ''
  chatLoading.value = false
  chatCompleted.value = false
  aiResults.value = []
}

async function loadMedicines() {
  loading.value = true
  try {
    const res = await getMedicineList(queryParams)
    medicines.value = res.data || []
    total.value = res.total || 0
  } catch {
    medicines.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function showDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getMedicineDetail(id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  loadMedicines()
}

onMounted(loadMedicines)
</script>

<style scoped>
.header-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.head-left{
  display:flex;
  align-items:center;
  gap: 10px;
  font-weight: 800;
  letter-spacing: 0.2px;
}
.filters{
  display:flex;
  align-items:center;
  gap: 8px;
  flex-wrap: wrap;
}
.filters :deep(.el-input__wrapper),
.filters :deep(.el-select__wrapper){
  background: rgba(15, 23, 42, 0.02);
  border: 1px solid rgba(15, 23, 42, 0.08);
  box-shadow: none;
}

.card-head{
  display:flex;
  align-items:center;
  justify-content: space-between;
  gap: 12px;
}

.ai-card :deep(.el-card__header){
  background: linear-gradient(180deg, rgba(47, 128, 237, 0.10), rgba(255,255,255,0));
}

.list-card :deep(.el-card__body){
  display:flex;
  flex-direction: column;
  gap: 12px;
}
.list-card :deep(.el-table){
  flex: 1;
}
.list-card :deep(.el-pagination){
  margin-top: auto;
}
.ai-card :deep(.el-card__body){
  display:flex;
  flex-direction: column;
}
.ai-card .ai-result{
  margin-top: 12px;
  flex-shrink: 0;
  overflow: auto;
  padding-right: 4px;
}

/* 聊天界面样式 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 200px;
  max-height: 420px;
}

.chat-message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.chat-avatar {
  flex-shrink: 0;
}

.chat-bubble {
  max-width: 80%;
}

.chat-content {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-message.assistant .chat-content {
  background: rgba(47, 128, 237, 0.08);
  color: #303133;
  border-bottom-left-radius: 4px;
}

.chat-message.user .chat-content {
  background: linear-gradient(135deg, rgba(47, 128, 237, 0.92) 0%, rgba(120, 87, 255, 0.92) 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

/* AI正在输入动画 */
.chat-typing {
  display: flex;
  gap: 5px;
  padding: 10px 14px;
  background: rgba(47, 128, 237, 0.08);
  border-radius: 14px;
  border-bottom-left-radius: 4px;
}

.chat-typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(47, 128, 237, 0.4);
  animation: typing 1.4s infinite both;
}

.chat-typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.chat-typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* 输入框区域 */
.chat-input-area {
  margin-top: 12px;
  flex-shrink: 0;
}

.chat-input-area :deep(.el-input-group__append) {
  padding: 0;
}

.chat-input-area :deep(.el-input-group__append .el-button) {
  margin: 0;
  border: none;
  border-radius: 0 4px 4px 0;
}

.ai-item-card{
  border-radius: 16px;
  background: rgba(255,255,255,0.72);
}
:deep(.ai-item-card .el-card__header){
  background: linear-gradient(180deg, rgba(47, 128, 237, 0.10), rgba(255,255,255,0));
}
.ai-item-card p { margin: 4px 0; font-size: 13px; color: #606266; }
</style>
