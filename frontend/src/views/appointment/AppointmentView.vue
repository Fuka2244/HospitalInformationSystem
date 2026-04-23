<template>
  <div class="page appointment-container">
    <div class="page-header">
      <div>
        <div class="page-title">预约挂号</div>
        <div class="page-subtitle">AI 智能导诊、医生排班与预约管理</div>
      </div>
      <div class="page-actions">
        <el-button type="primary" @click="showCreate = true">新建预约</el-button>
      </div>
    </div>
    <el-row class="fill-row" :gutter="20">
      <!-- AI智能导诊 -->
      <el-col :xs="24" :lg="8" class="fill-col">
        <el-card class="ai-card fill-card" shadow="hover">
          <template #header>
            <div class="card-head">
              <span>AI 智能导诊</span>
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
          <div v-if="recommendation && chatCompleted" class="ai-result">
            <el-divider>推荐结果</el-divider>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="推荐科室">
                <el-tag type="success">{{ recommendation.department }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="推荐医生">{{ recommendation.doctor }}</el-descriptions-item>
              <el-descriptions-item label="推荐日期">{{ recommendation.recommendedDate }}</el-descriptions-item>
              <el-descriptions-item label="推荐时段">{{ recommendation.recommendedTime }}</el-descriptions-item>
              <el-descriptions-item label="推荐理由">{{ recommendation.reason }}</el-descriptions-item>
            </el-descriptions>

            <!-- 可用排班列表 -->
            <el-divider>可用时间段</el-divider>
            <div v-if="aiAvailableSchedules.length > 0" class="schedule-list">
              <div
                v-for="schedule in aiAvailableSchedules"
                :key="schedule.id"
                class="schedule-item"
                :class="{ selected: selectedSchedule?.id === schedule.id }"
                @click="selectSchedule(schedule)"
              >
                <div class="schedule-time">{{ schedule.timeSlot }}</div>
                <div class="schedule-info">余号: {{ schedule.maxPatients - schedule.bookedCount }}</div>
              </div>
              <el-button type="primary" :disabled="!selectedSchedule" style="width: 100%; margin-top: 12px" @click="handleAiBook">
                确认预约
              </el-button>
            </div>
            <div v-else class="no-schedule">
              <el-text type="info">该医生在推荐日期无可用排班，建议选择其他日期或医生</el-text>
            </div>
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

      <!-- 预约列表 -->
      <el-col :xs="24" :lg="16" class="fill-col">
        <el-card class="list-card fill-card" shadow="hover">
          <template #header>
            <div class="header-row">
              <div class="head-left">
                <span>我的预约</span>
                <el-tag effect="light" type="info" size="small">共 {{ store.total }} 条</el-tag>
              </div>
            </div>
          </template>
          <el-table :data="store.appointments" v-loading="store.loading" stripe>
            <el-table-column prop="appointmentDate" label="预约日期" width="120" />
            <el-table-column prop="timeSlot" label="时段" width="130" />
            <el-table-column prop="departmentName" label="科室" width="110" />
            <el-table-column prop="doctorName" label="医生" width="100" />
            <el-table-column prop="appointmentType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.appointmentType === 'DOCTOR' ? '' : 'warning'" size="small">
                  {{ row.appointmentType === 'DOCTOR' ? '医生预约' : '检查预约' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="statusTagType[row.status]" size="small">{{ statusMap[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="160" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 0">
                  <el-button link type="warning" @click="handleCancel(row)">取消</el-button>
                  <el-button link type="primary" @click="handleReschedule(row)">改期</el-button>
                </template>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="store.total"
            layout="total, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @change="loadList"
          />
        </el-card>
      </el-col>
    </el-row>

      <!-- 新建预约对话框 -->
    <el-dialog v-model="showCreate" title="新建预约" width="1400px" @open="resetCreateForm">
      <!-- 筛选工具栏 -->
      <div class="filter-toolbar">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="科室">
            <el-select v-model="filterDepartment" placeholder="全部科室" clearable style="width: 180px">
              <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="日期">
            <el-date-picker v-model="filterDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" :disabled-date="disablePastDate" style="width: 180px" />
          </el-form-item>
          <el-form-item label="时段">
            <el-select v-model="filterTimeSlot" placeholder="全部时段" clearable style="width: 180px">
              <el-option label="上午 (08:00-12:00)" value="08:00-12:00" />
              <el-option label="下午 (14:00-18:00)" value="14:00-18:00" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="applyFilter">筛选</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 医生列表 -->
      <div class="doctor-table-container">
        <div v-if="filteredDoctorList.length > 0" class="doctor-table">
          <div
            v-for="doctor in filteredDoctorList"
            :key="doctor.id"
            class="doctor-row"
            :class="{ selected: selectedDoctorId === doctor.id }"
            @click="selectDoctor(doctor)"
          >
            <div class="doctor-avatar">
              <el-avatar :size="60" :icon="'UserFilled'" />
            </div>
            <div class="doctor-info-section">
              <div class="doctor-name">{{ doctor.name }}</div>
              <div class="doctor-meta">
                <el-tag size="small" type="primary">{{ doctor.title }}</el-tag>
                <span class="doctor-dept">{{ getDepartmentName(doctor.departmentId) }}</span>
                <span class="doctor-gender">{{ doctor.gender }}</span>
                <span class="doctor-age">{{ doctor.age }}岁</span>
              </div>
              <div class="doctor-specialty">专长：{{ doctor.specialty }}</div>
            </div>
            <div class="doctor-action-section">
              <el-button type="primary" @click.stop="showDoctorSchedule(doctor)">
                查看排班
              </el-button>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">
          <el-empty description="暂无符合条件的医生" />
        </div>
      </div>

      <!-- 排班选择弹窗 -->
      <el-dialog v-model="showScheduleDialog" title="选择就诊时段" width="1000px" append-to-body>
        <div v-if="currentDoctor" class="schedule-dialog-content">
          <div class="schedule-doctor-info">
            <el-avatar :size="80" :icon="'UserFilled'" />
            <div class="doctor-detail">
              <div class="doctor-detail-name">{{ currentDoctor.name }}</div>
              <div class="doctor-detail-meta">
                <el-tag type="primary">{{ currentDoctor.title }}</el-tag>
                <span>{{ getDepartmentName(currentDoctor.departmentId) }}</span>
              </div>
            </div>
          </div>
          <div v-if="doctorAvailableSlots.length > 0" class="schedule-slots">
            <div
              v-for="slot in doctorAvailableSlots"
              :key="slot.id"
              class="schedule-slot-card"
              :class="{ selected: selectedSlot?.id === slot.id }"
              @click="selectScheduleSlot(slot)"
            >
              <div class="slot-header">
                <div class="slot-date">{{ slot.scheduleDate }}</div>
                <div class="slot-time">{{ slot.timeSlot }}</div>
              </div>
              <div class="slot-status">
                <el-tag :type="slot.bookedCount >= slot.maxPatients ? 'danger' : 'success'" size="small">
                  {{ slot.bookedCount >= slot.maxPatients ? '已满' : '可预约' }}
                </el-tag>
                <span class="slot-count">{{ slot.bookedCount }}/{{ slot.maxPatients }}</span>
              </div>
            </div>
          </div>
          <div v-else class="no-schedule">
            <el-empty description="该医生暂无可用排班" />
          </div>
        </div>
        <template #footer>
          <el-button @click="showScheduleDialog = false">取消</el-button>
          <el-button type="primary" :disabled="!selectedSlot" @click="confirmSchedule">
            确认选择
          </el-button>
        </template>
      </el-dialog>

      <!-- 已选时段预览 -->
      <div v-if="selectedSlot" class="selected-preview">
        <div class="preview-header">已选时段</div>
        <div class="preview-content">
          <div class="preview-item">
            <span class="preview-label">医生：</span>
            <span class="preview-value">{{ selectedSlot.doctorName }}</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">科室：</span>
            <span class="preview-value">{{ selectedSlot.departmentName }}</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">日期：</span>
            <span class="preview-value">{{ selectedSlot.scheduleDate }}</span>
          </div>
          <div class="preview-item">
            <span class="preview-label">时段：</span>
            <span class="preview-value">{{ selectedSlot.timeSlot }}</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" :disabled="!selectedSlot" @click="handleCreate">
          确认预约
        </el-button>
      </template>
    </el-dialog>

    <!-- 取消预约对话框 -->
    <el-dialog v-model="cancelVisible" title="取消预约" width="400px">
      <el-form label-width="80px">
        <el-form-item label="取消原因">
          <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="请输入取消原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelVisible = false">返回</el-button>
        <el-button type="danger" :loading="cancelLoading" @click="confirmCancel">确认取消</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAppointmentStore } from '@/stores/appointment'
import { getDepartmentList, getDoctorList } from '@/api/department'
import type { Appointment, AppointmentCreateDto, AppointmentRecommendation, ChatMessageDto, Department, Doctor, DoctorSchedule } from '@/types'

const store = useAppointmentStore()
const showCreate = ref(false)
const createLoading = ref(false)
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelReason = ref('')
const cancelTarget = ref<Appointment | null>(null)
const recommendation = ref<AppointmentRecommendation | null>(null)
const selectedSchedule = ref<DoctorSchedule | null>(null)
const aiAvailableSchedules = ref<DoctorSchedule[]>([])
const departments = ref<Department[]>([])
const allDoctors = ref<Doctor[]>([])

// 聊天相关状态
const chatMessages = ref<ChatMessageDto[]>([
  { role: 'assistant', content: '您好！我是AI导诊助手，请问您今天哪里不舒服？请描述一下您的症状。' }
])
const chatInput = ref('')
const chatLoading = ref(false)
const chatCompleted = ref(false)
const chatMessagesRef = ref<HTMLElement | null>(null)

// 筛选状态
const filterDepartment = ref<number | undefined>(undefined)
const filterDate = ref<string>('')
const filterTimeSlot = ref<string>('')

// 排班弹窗
const showScheduleDialog = ref(false)
const currentDoctor = ref<Doctor | null>(null)
const selectedDoctorId = ref<number | undefined>(undefined)
const selectedSlot = ref<DoctorSchedule | null>(null)

const statusMap: Record<number, string> = { 0: '已预约', 1: '已完成', 2: '已取消' }
const statusTagType: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }

// 医生可用排班
const doctorAvailableSlots = ref<any[]>([])

// 根据条件筛选医生列表
const filteredDoctorList = computed(() => {
  let result = allDoctors.value

  // 按科室筛选
  if (filterDepartment.value) {
    result = result.filter(d => d.departmentId === filterDepartment.value)
  }

  // 如果选择了日期和时段，还需要有该日期和时段的可用排班
  if (filterDate.value && filterTimeSlot.value) {
    result = result.filter(d => {
      const hasSchedule = doctorAvailableSlots.value.some(s =>
        s.doctorId === d.id &&
        s.scheduleDate === filterDate.value &&
        s.timeSlot === filterTimeSlot.value &&
        s.bookedCount < s.maxPatients
      )
      return hasSchedule
    })
  } else if (filterDate.value) {
    // 如果只选择了日期，显示有该日期排班的医生
    result = result.filter(d => {
      const hasSchedule = doctorAvailableSlots.value.some(s =>
        s.doctorId === d.id &&
        s.scheduleDate === filterDate.value &&
        s.bookedCount < s.maxPatients
      )
      return hasSchedule
    })
  }

  return result
})

const queryParams = reactive({ page: 1, size: 10, status: undefined as number | undefined })

function disablePastDate(date: Date) {
  return date.getTime() < Date.now() - 86400000
}

function getDepartmentName(deptId: number): string {
  const dept = departments.value.find(d => d.id === deptId)
  return dept?.name || '未知科室'
}

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

  // 添加用户消息
  chatMessages.value.push({ role: 'user', content: msg })
  chatInput.value = ''
  chatLoading.value = true
  scrollToBottom()

  try {
    // 构建历史消息（不包含当前消息）
    const history = chatMessages.value.slice(0, -1)
    const res = await store.aiTriageChat(msg, history)

    // 添加AI回复
    chatMessages.value.push({ role: 'assistant', content: res.reply })
    scrollToBottom()

    if (res.completed) {
      chatCompleted.value = true
      recommendation.value = res.recommendation || null
      aiAvailableSchedules.value = res.availableSchedules || []
    }
  } catch (error) {
    ElMessage.error('AI导诊服务暂时不可用，请稍后重试')
    chatMessages.value.push({ role: 'assistant', content: '抱歉，我暂时无法响应，请稍后再试。' })
    scrollToBottom()
  } finally {
    chatLoading.value = false
  }
}

// 重置聊天
function resetChat() {
  chatMessages.value = [
    { role: 'assistant', content: '您好！我是AI导诊助手，请问您今天哪里不舒服？请描述一下您的症状。' }
  ]
  chatInput.value = ''
  chatLoading.value = false
  chatCompleted.value = false
  recommendation.value = null
  selectedSchedule.value = null
  aiAvailableSchedules.value = []
}

// 应用筛选条件
async function applyFilter() {
  if (filterDate.value) {
    // 如果选择了日期，查询该日期的排班
    await store.fetchSchedules({
      date: filterDate.value,
    })

    // 只保留可用的排班
    doctorAvailableSlots.value = store.schedules.filter(s => s.bookedCount < s.maxPatients)
  } else {
    doctorAvailableSlots.value = []
  }
}

// 重置筛选条件
function resetFilter() {
  filterDepartment.value = undefined
  filterDate.value = ''
  filterTimeSlot.value = ''
  doctorAvailableSlots.value = []
  selectedDoctorId.value = undefined
  selectedSlot.value = null
}

// 选择医生
function selectDoctor(doctor: Doctor) {
  selectedDoctorId.value = doctor.id
}

// 查看医生排班
async function showDoctorSchedule(doctor: Doctor) {
  currentDoctor.value = doctor

  // 查询该医生的排班，有日期按日期查，无日期查未来7天
  await store.fetchSchedules({
    doctorId: doctor.id,
    date: filterDate.value || undefined,
  })

  // 过滤可用排班
  doctorAvailableSlots.value = store.schedules.filter(s => s.bookedCount < s.maxPatients)

  showScheduleDialog.value = true
}

// 选择排班时段
function selectScheduleSlot(slot: DoctorSchedule) {
  selectedSlot.value = slot
}

// 确认选择时段
function confirmSchedule() {
  if (!selectedSlot.value) {
    ElMessage.warning('请选择时段')
    return
  }
  showScheduleDialog.value = false
}

function selectSchedule(schedule: DoctorSchedule) {
  selectedSchedule.value = schedule
}

async function handleAiBook() {
  if (!selectedSchedule.value || !recommendation.value) {
    return
  }

  const createDto: AppointmentCreateDto = {
    departmentId: recommendation.value.departmentId!,
    doctorId: recommendation.value.doctorId!,
    appointmentType: 'DOCTOR',
    examinationType: undefined,
    appointmentDate: selectedSchedule.value.scheduleDate,
    timeSlot: selectedSchedule.value.timeSlot,
  }

  try {
    await store.create(createDto)
    ElMessage.success('预约成功')
    resetChat()
    loadList()
  } catch (error) {
    ElMessage.error('预约失败，请重试')
  }
}

async function handleCreate() {
  if (!selectedSlot.value) {
    ElMessage.warning('请先选择时段')
    return
  }

  const createDto: AppointmentCreateDto = {
    departmentId: selectedSlot.value.departmentId,
    doctorId: selectedSlot.value.doctorId,
    appointmentType: 'DOCTOR',
    examinationType: undefined,
    appointmentDate: selectedSlot.value.scheduleDate,
    timeSlot: selectedSlot.value.timeSlot,
  }

  createLoading.value = true
  try {
    await store.create(createDto)
    ElMessage.success('预约成功')
    showCreate.value = false
    loadList()
  } catch (error) {
    ElMessage.error('预约失败，请重试')
  } finally {
    createLoading.value = false
  }
}

function handleCancel(row: Appointment) {
  cancelTarget.value = row
  cancelReason.value = ''
  cancelVisible.value = true
}

async function confirmCancel() {
  if (!cancelTarget.value) return
  cancelLoading.value = true
  try {
    await store.cancel(cancelTarget.value.id, cancelReason.value)
    ElMessage.success('已取消预约')
    cancelVisible.value = false
    loadList()
  } finally {
    cancelLoading.value = false
  }
}

async function handleReschedule(row: Appointment) {
  try {
    await ElMessageBox.confirm('改期将取消当前预约并创建新预约，是否继续？', '改期确认', { type: 'warning' })
    filterDepartment.value = row.departmentId
    filterDate.value = ''
    filterTimeSlot.value = ''
    selectedDoctorId.value = row.doctorId ?? undefined
    selectedSlot.value = null
    showCreate.value = true
  } catch { /* 用户取消 */ }
}

function resetCreateForm() {
  filterDepartment.value = undefined
  filterDate.value = ''
  filterTimeSlot.value = ''
  doctorAvailableSlots.value = []
  selectedDoctorId.value = undefined
  selectedSlot.value = null
  showScheduleDialog.value = false
  currentDoctor.value = null
}

async function loadList() {
  await store.fetchList(queryParams)
}

onMounted(async () => {
  try {
    const deptRes = await getDepartmentList()
    departments.value = deptRes.data || []
  } catch { /* 后端不可用 */ }

  try {
    const doctorRes = await getDoctorList()
    allDoctors.value = doctorRes.data || []
  } catch { /* 后端不可用 */ }

  loadList()
})
</script>

<style scoped>
.card-head{
  display:flex;
  align-items:center;
  justify-content: space-between;
  gap: 12px;
}
.header-row { display: flex; justify-content: space-between; align-items: center; }
.head-left{
  display:flex;
  align-items:center;
  gap: 10px;
  font-weight: 800;
  letter-spacing: 0.2px;
}
.ai-card :deep(.el-card__header){
  background: linear-gradient(180deg, rgba(47, 128, 237, 0.10), rgba(255,255,255,0));
}
.list-card :deep(.el-card__header){
  background: linear-gradient(180deg, rgba(120, 87, 255, 0.08), rgba(255,255,255,0));
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

/* 筛选工具栏 */
.filter-toolbar {
  padding: 16px;
  background: rgba(15, 23, 42, 0.02);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  margin-bottom: 20px;
}

.filter-form {
  margin: 0;
}

/* 医生表格 */
.doctor-table-container {
  max-height: 500px;
  overflow-y: auto;
}

.doctor-table {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.doctor-row {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 16px;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(10px);
}

.doctor-row:hover {
  border-color: rgba(47, 128, 237, 0.20);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.10);
  transform: translateY(-1px);
}

.doctor-row.selected {
  border-color: rgba(47, 128, 237, 0.30);
  background:
    radial-gradient(700px 340px at 20% 0%, rgba(47, 128, 237, 0.42), transparent 60%),
    radial-gradient(700px 340px at 100% 20%, rgba(120, 87, 255, 0.36), transparent 60%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.92) 0%, rgba(15, 23, 42, 0.84) 100%);
  color: white;
}

.doctor-avatar {
  flex-shrink: 0;
}

.doctor-info-section {
  flex: 1;
}

.doctor-row .doctor-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 8px;
}

.doctor-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}

.doctor-dept {
  color: #909399;
}

.doctor-row.selected .doctor-dept {
  color: rgba(255, 255, 255, 0.9);
}

.doctor-gender,
.doctor-age {
  color: #606266;
}

.doctor-row.selected .doctor-gender,
.doctor-row.selected .doctor-age {
  color: rgba(255, 255, 255, 0.9);
}

.doctor-specialty {
  font-size: 13px;
  color: #909399;
  line-height: 1.5;
}

.doctor-row.selected .doctor-specialty {
  color: rgba(255, 255, 255, 0.85);
}

.doctor-action-section {
  flex-shrink: 0;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
}

/* 排班选择弹窗 */
.schedule-dialog-content {
  padding: 20px;
}

.schedule-doctor-info {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background:
    radial-gradient(700px 340px at 20% 0%, rgba(47, 128, 237, 0.52), transparent 60%),
    radial-gradient(700px 340px at 100% 20%, rgba(120, 87, 255, 0.42), transparent 60%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.92) 0%, rgba(15, 23, 42, 0.84) 100%);
  border-radius: 18px;
  color: white;
  margin-bottom: 24px;
  border: 1px solid rgba(255,255,255,0.16);
}

.doctor-detail-name {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 8px;
}

.doctor-detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.schedule-slots {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
  max-height: 400px;
  overflow-y: auto;
}

.schedule-slot-card {
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 16px;
  padding: 20px;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(10px);
}

.schedule-slot-card:hover {
  border-color: rgba(47, 128, 237, 0.20);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.10);
  transform: translateY(-2px);
}

.schedule-slot-card.selected {
  border-color: rgba(47, 128, 237, 0.28);
  background: linear-gradient(135deg, rgba(47, 128, 237, 0.92) 0%, rgba(120, 87, 255, 0.92) 100%);
  color: white;
}

.slot-header {
  margin-bottom: 12px;
}

.slot-date {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 6px;
}

.slot-time {
  font-size: 14px;
  color: #606266;
}

.schedule-slot-card.selected .slot-time {
  color: rgba(255, 255, 255, 0.9);
}

.slot-status {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.slot-count {
  font-size: 13px;
  color: #909399;
}

.schedule-slot-card.selected .slot-count {
  color: rgba(255, 255, 255, 0.9);
}

.no-schedule {
  padding: 60px 20px;
  text-align: center;
}

/* 已选时段预览 */
.selected-preview {
  margin-top: 20px;
  padding: 20px;
  background: rgba(255,255,255,0.72);
  border-radius: 16px;
  border: 1px solid rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(10px);
}

.preview-header {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 12px;
  color: #303133;
}

.preview-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.preview-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-label {
  font-weight: 500;
  color: #606266;
}

.preview-value {
  color: #303133;
  font-weight: bold;
}

/* AI可用时段 */
.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.schedule-item {
  padding: 12px;
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 14px;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(15, 23, 42, 0.02);
}

.schedule-item:hover {
  border-color: rgba(47, 128, 237, 0.20);
  background: rgba(47, 128, 237, 0.06);
  transform: translateY(-1px);
}

.schedule-item.selected {
  border-color: rgba(47, 128, 237, 0.25);
  background: linear-gradient(135deg, rgba(47, 128, 237, 0.92) 0%, rgba(120, 87, 255, 0.92) 100%);
  color: white;
}

.schedule-item.selected .schedule-info {
  color: white;
}

.schedule-time {
  font-weight: bold;
  font-size: 16px;
}

.schedule-info {
  font-size: 14px;
  color: #909399;
}

.no-schedule {
  padding: 20px;
  text-align: center;
}
</style>
