<template>
  <div class="page billing-container">
    <div class="page-header">
      <div>
        <div class="page-title">费用查询</div>
        <div class="page-subtitle">按类型/状态/日期筛选账单，并用 AI 获取费用解释</div>
      </div>
    </div>

    <div class="stats">
      <div class="stat-card">
        <div class="stat-label">本页费用总额</div>
        <div class="stat-value">¥{{ pageTotal.toFixed(2) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已支付</div>
        <div class="stat-value success">¥{{ paidTotal.toFixed(2) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">未支付</div>
        <div class="stat-value danger">¥{{ unpaidTotal.toFixed(2) }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本页账单数</div>
        <div class="stat-value">{{ billings.length }}</div>
      </div>
    </div>

    <el-row class="fill-row" :gutter="20">
      <!-- 费用列表 -->
      <el-col :xs="24" :lg="16" class="fill-col">
        <el-card class="fill-card list-card" shadow="hover">
          <template #header>
            <div class="header-row">
              <div class="head-left">
                <span>医疗费用</span>
                <el-tag effect="light" type="info" size="small">共 {{ total }} 条</el-tag>
              </div>
              <div class="filters">
                <el-select v-model="queryParams.itemType" placeholder="费用类型" clearable style="width: 130px; margin-right: 8px">
                  <el-option label="挂号" value="REGISTRATION" />
                  <el-option label="检查" value="EXAMINATION" />
                  <el-option label="药品" value="MEDICINE" />
                  <el-option label="其他" value="OTHER" />
                </el-select>
                <el-select v-model="queryParams.status" placeholder="支付状态" clearable style="width: 120px; margin-right: 8px">
                  <el-option label="未支付" :value="0" />
                  <el-option label="已支付" :value="1" />
                  <el-option label="已退款" :value="2" />
                </el-select>
                <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="margin-right: 8px" />
                <el-button type="primary" @click="handleSearch">查询</el-button>
              </div>
            </div>
          </template>

          <el-table :data="billings" v-loading="loading" stripe>
            <el-table-column prop="createTime" label="时间" width="170" />
            <el-table-column prop="itemType" label="类型" width="90">
              <template #default="{ row }">
                <el-tag :type="itemTypeTag[row.itemType]" size="small">{{ itemTypeMap[row.itemType] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="itemName" label="项目" min-width="150" />
            <el-table-column prop="amount" label="金额" width="100">
              <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTagType[row.status]" size="small">{{ statusMap[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="description" label="说明" min-width="150" show-overflow-tooltip />
          </el-table>

          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            layout="total, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @change="loadBillings"
          />
        </el-card>
      </el-col>

      <!-- 费用可视化 + AI对话 -->
      <el-col :xs="24" :lg="8" class="fill-col">
        <div class="right-stack">
          <el-card class="pie-card" shadow="hover">
            <template #header>
              <div class="header-row">
                <span>费用构成（按类型）</span>
                <el-tag effect="light" type="info" size="small">当前筛选</el-tag>
              </div>
            </template>
            <BillingPieChart :items="pieItems" @select="handlePieSelect" />
            <div class="pie-hint">悬浮高亮，点击查看具体费用</div>
          </el-card>

          <el-card class="ai-card" shadow="hover">
            <template #header>
              <div class="card-head">
                <span>AI 费用解释</span>
                <div style="display:flex;gap:8px;align-items:center;">
                  <el-tag v-if="chatCompleted" effect="light" type="success" size="small">解释完成</el-tag>
                  <el-tag v-else effect="light" type="warning" size="small">仅供参考</el-tag>
                  <el-button v-if="chatMessages.length > 1" link type="info" size="small" @click="resetChat">重新开始</el-button>
                </div>
              </div>
            </template>

            <!-- 日期范围选择 -->
            <el-date-picker v-model="chatDateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width: 100%; margin-bottom: 12px" />

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

            <!-- 解释结果区域 -->
            <div v-if="explanation && chatCompleted" class="ai-result">
              <el-divider>解释结果</el-divider>
              <el-descriptions :column="1" border size="small">
                <el-descriptions-item label="费用总计">
                  <span style="font-size: 18px; color: #e6a23c; font-weight: bold">¥{{ explanation.totalAmount?.toFixed(2) }}</span>
                </el-descriptions-item>
                <el-descriptions-item label="费用构成">{{ explanation.breakdown }}</el-descriptions-item>
                <el-descriptions-item label="详细解释">{{ explanation.explanation }}</el-descriptions-item>
                <el-descriptions-item label="建议">{{ explanation.suggestion }}</el-descriptions-item>
              </el-descriptions>
            </div>

            <!-- 输入框 -->
            <div class="chat-input-area">
              <el-input
                v-model="chatInput"
                placeholder="请输入您关于费用的问题..."
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
        </div>
      </el-col>
    </el-row>

    <el-dialog v-model="pieDialogVisible" title="费用详情" width="420px">
      <el-descriptions v-if="selectedSlice" :column="1" border size="small">
        <el-descriptions-item label="分类">{{ selectedSlice.label }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ selectedSlice.value.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="占比">{{ (selectedSlice.percent * 100).toFixed(1) }}%</el-descriptions-item>
        <el-descriptions-item v-if="selectedSlice.count != null" label="条目数">{{ selectedSlice.count }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { getBillingList, aiBillingChat } from '@/api/billing'
import BillingPieChart, { type PieItem } from './components/BillingPieChart.vue'
import type { Billing, BillingQueryParams, BillingExplanation, ChatMessageDto } from '@/types'

const loading = ref(false)
const billings = ref<Billing[]>([])
const total = ref(0)
const dateRange = ref<[string, string] | null>(null)

const pieDialogVisible = ref(false)
const selectedSlice = ref<(PieItem & { percent: number }) | null>(null)

const itemTypeMap: Record<string, string> = { REGISTRATION: '挂号', EXAMINATION: '检查', MEDICINE: '药品', OTHER: '其他' }
const itemTypeTag: Record<string, string> = { REGISTRATION: '', EXAMINATION: 'warning', MEDICINE: 'success', OTHER: 'info' }
const statusMap: Record<number, string> = { 0: '未支付', 1: '已支付', 2: '已退款' }
const statusTagType: Record<number, string> = { 0: 'danger', 1: 'success', 2: 'info' }

const typeColor: Record<string, string> = {
  REGISTRATION: '#6366f1',
  EXAMINATION: '#f59e0b',
  MEDICINE: '#22c55e',
  OTHER: '#38bdf8',
}

const queryParams = reactive<BillingQueryParams>({ page: 1, size: 10 })

// 聊天相关状态
const chatMessages = ref<ChatMessageDto[]>([
  { role: 'assistant', content: '您好！我是AI费用助手，已获取您的费用数据，请直接提出您的问题，我会基于您的实际费用进行解答。' }
])
const chatInput = ref('')
const chatLoading = ref(false)
const chatCompleted = ref(false)
const chatMessagesRef = ref<HTMLElement | null>(null)
const chatDateRange = ref<[string, string] | null>(null)
const explanation = ref<BillingExplanation | null>(null)

const pageTotal = computed(() => billings.value.reduce((sum, item) => sum + (item.amount || 0), 0))
const paidTotal = computed(() => billings.value.filter(b => b.status === 1).reduce((sum, item) => sum + (item.amount || 0), 0))
const unpaidTotal = computed(() => billings.value.filter(b => b.status === 0).reduce((sum, item) => sum + (item.amount || 0), 0))

const pieItems = computed<PieItem[]>(() => {
  const groups: Record<string, { amount: number; count: number }> = {}
  for (const b of billings.value) {
    const key = b.itemType
    if (!groups[key]) groups[key] = { amount: 0, count: 0 }
    groups[key].amount += b.amount || 0
    groups[key].count += 1
  }
  return Object.entries(groups)
    .map(([key, val]) => ({
      key,
      label: itemTypeMap[key] || key,
      value: val.amount,
      color: typeColor[key] || '#94a3b8',
      count: val.count,
    }))
    .filter(i => i.value > 0)
})

// 滚动聊天到底部
async function scrollToBottom() {
  await nextTick()
  if (chatMessagesRef.value) {
    chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight
  }
}

// 发送聊天消息
async function sendChatMessage() {
  const msg = chatInput.value.trim()
  if (!msg || chatLoading.value || chatCompleted.value) return

  chatMessages.value.push({ role: 'user', content: msg })
  chatInput.value = ''
  chatLoading.value = true
  scrollToBottom()

  try {
    const history = chatMessages.value.slice(0, -1)
    const params: any = { message: msg, history }
    if (chatDateRange.value) {
      params.startDate = chatDateRange.value[0]
      params.endDate = chatDateRange.value[1]
    }
    const res = await aiBillingChat(params)

    chatMessages.value.push({ role: 'assistant', content: res.data.reply })
    scrollToBottom()

    if (res.data.completed) {
      chatCompleted.value = true
      explanation.value = res.data.explanation || null
    }
  } catch (error) {
    ElMessage.error('AI费用服务暂时不可用，请稍后重试')
    chatMessages.value.push({ role: 'assistant', content: '抱歉，我暂时无法响应，请稍后再试。' })
    scrollToBottom()
  } finally {
    chatLoading.value = false
  }
}

// 重置聊天
function resetChat() {
  chatMessages.value = [
    { role: 'assistant', content: '您好！我是AI费用助手，已获取您的费用数据，请直接提出您的问题，我会基于您的实际费用进行解答。' }
  ]
  chatInput.value = ''
  chatLoading.value = false
  chatCompleted.value = false
  explanation.value = null
}

async function loadBillings() {
  loading.value = true
  try {
    if (dateRange.value) {
      queryParams.startDate = dateRange.value[0]
      queryParams.endDate = dateRange.value[1]
    } else {
      queryParams.startDate = undefined
      queryParams.endDate = undefined
    }

    const listRes = await getBillingList(queryParams)

    billings.value = listRes.data || []
    total.value = listRes.total || 0
  } catch {
    billings.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  loadBillings()
}

function handlePieSelect(item: PieItem & { percent: number }) {
  selectedSlice.value = item
  pieDialogVisible.value = true
}

onMounted(loadBillings)
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
.filters :deep(.el-select__wrapper),
.filters :deep(.el-range-editor.el-input__wrapper){
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
.right-stack{
  width: 100%;
  display:flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  min-height: 0;
}
.ai-card{ flex: 1; min-height: 0; }
.pie-card :deep(.el-card__body){ padding-top: 10px; }
.pie-hint{
  margin-top: 8px;
  color: rgba(15, 23, 42, 0.55);
  font-size: 12px;
  letter-spacing: 0.2px;
}

.stats{
  display:grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin: 6px 0 16px;
}
.stat-card{
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(10px);
  box-shadow: 0 10px 26px rgba(15, 23, 42, 0.08);
  padding: 14px 14px 12px;
}
.stat-label{
  color: rgba(15, 23, 42, 0.55);
  font-size: 12px;
  letter-spacing: 0.2px;
}
.stat-value{
  margin-top: 6px;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 0.2px;
}
.stat-value.success{ color: #16a34a; }
.stat-value.danger{ color: #dc2626; }
@media (max-width: 1200px){
  .stats{ grid-template-columns: repeat(2, 1fr); }
}

/* 聊天界面样式 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: 160px;
  max-height: 360px;
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
</style>
