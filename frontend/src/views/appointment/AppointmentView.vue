<template>
  <div class="appointment-page">
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="bg-circle bg-circle-1"></div>
      <div class="bg-circle bg-circle-2"></div>
      <div class="bg-circle bg-circle-3"></div>
    </div>

    <div class="page-content">
      <!-- 页面头部 -->
      <div class="page-header hero-header">
        <div class="hero-left">
          <div class="hero-title-row">
            <el-icon class="hero-icon"><Calendar /></el-icon>
            <span class="hero-title">预约挂号</span>
          </div>
          <p class="hero-subtitle">AI 智能导诊 · 医生排班与预约管理</p>
        </div>
        <div class="hero-actions">
          <el-button class="hero-action-btn" type="primary" size="large" @click="showCreate = true">
            <el-icon><Plus /></el-icon>新建预约
          </el-button>
        </div>
      </div>

      <!-- 主内容区域 -->
      <div class="main-grid">
        <!-- AI智能导诊 -->
        <div class="section-card ai-section">
          <div class="section-header">
            <div class="section-title">
              <el-icon class="section-icon"><ChatDotRound /></el-icon>
              <span>AI 智能导诊</span>
            </div>
            <div class="section-tags">
              <el-tag v-if="chatCompleted" effect="light" type="success" size="small" round>推荐完成</el-tag>
              <el-tag v-else effect="light" type="warning" size="small" round>在线咨询</el-tag>
              <el-button v-if="chatMessages.length > 1" link type="info" size="small" @click="resetChat">重新开始</el-button>
            </div>
          </div>

          <!-- 聊天消息列表 -->
          <div class="chat-container">
            <div class="chat-messages" ref="chatMessagesRef">
              <div v-for="(msg, idx) in chatMessages" :key="idx" class="chat-message" :class="msg.role">
                <div class="chat-avatar">
                  <el-avatar v-if="msg.role === 'assistant'" :size="36" class="avatar-ai">
                    AI
                  </el-avatar>
                  <el-avatar v-else :size="36" class="avatar-user">
                    <el-icon><User /></el-icon>
                  </el-avatar>
                </div>
                <div class="chat-bubble">
                  <div class="chat-content">{{ msg.content }}</div>
                </div>
              </div>
              <!-- AI正在输入提示 -->
              <div v-if="chatLoading" class="chat-message assistant">
                <div class="chat-avatar">
                  <el-avatar :size="36" class="avatar-ai">
                    AI
                  </el-avatar>
                </div>
                <div class="chat-bubble">
                  <div class="chat-typing">
                    <span></span><span></span><span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 推荐结果区域 -->
          <div v-if="recommendation && chatCompleted" class="ai-result">
            <el-divider content-position="left">
              <el-icon><Star /></el-icon> 推荐结果
            </el-divider>
            <el-descriptions :column="1" border size="small" class="recommend-desc">
              <el-descriptions-item label="推荐科室">
                <el-tag type="success" effect="dark" size="small">{{ recommendation.department }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="推荐理由">{{ recommendation.reason }}</el-descriptions-item>
            </el-descriptions>

            <!-- 医生选择区域 -->
            <template v-if="recommendation.availableDoctors && recommendation.availableDoctors.length > 0">
              <el-divider content-position="left">
                <el-icon><User /></el-icon> 选择医生
              </el-divider>
              <div class="doctor-select-list">
                <div
                  v-for="doc in recommendation.availableDoctors"
                  :key="doc.doctorId"
                  class="doctor-select-card"
                  :class="{ selected: selectedDoctorForBooking?.doctorId === doc.doctorId }"
                  @click="selectedDoctorForBooking = doc; selectedSlotForBooking = null"
                >
                  <div class="doctor-select-info">
                    <div class="doctor-select-name">{{ doc.doctorName }}</div>
                    <div class="doctor-select-meta">
                      <el-tag size="small" effect="light">{{ doc.title }}</el-tag>
                      <span class="doctor-select-specialty">擅长：{{ doc.specialty }}</span>
                    </div>
                  </div>
                  <div class="doctor-select-badge">
                    <el-tag size="small" type="info">{{ doc.schedules.length }}个时段</el-tag>
                  </div>
                </div>
              </div>

              <!-- 选中医生后的排班选择 -->
              <template v-if="selectedDoctorForBooking">
                <el-divider content-position="left">
                  <el-icon><Clock /></el-icon> 选择就诊时段
                </el-divider>
                <div class="schedule-list">
                  <div
                    v-for="schedule in selectedDoctorForBooking.schedules"
                    :key="schedule.id"
                    class="schedule-item"
                    :class="{ selected: selectedSlotForBooking?.id === schedule.id }"
                    @click="selectedSlotForBooking = schedule"
                  >
                    <div class="schedule-date">{{ schedule.scheduleDate }}</div>
                    <div class="schedule-time">{{ schedule.timeSlot }}</div>
                    <div class="schedule-info">
                      <el-tag size="small" :type="schedule.bookedCount >= schedule.maxPatients ? 'danger' : 'success'" effect="light">
                        {{ schedule.bookedCount >= schedule.maxPatients ? '已满' : '余号' }}
                        {{ schedule.maxPatients - schedule.bookedCount }}
                      </el-tag>
                    </div>
                  </div>
                  <el-button type="primary" :disabled="!selectedSlotForBooking" class="confirm-btn" @click="handleAiBook">
                    <el-icon><Check /></el-icon> 确认预约
                  </el-button>
                </div>
              </template>
            </template>
            <template v-else>
              <el-divider content-position="left">
                <el-icon><Warning /></el-icon> 可用时间段
              </el-divider>
              <div class="no-schedule">
                <el-empty description="该科室未来7天暂无可用排班" :image-size="60" />
              </div>
            </template>
          </div>

          <!-- 输入框 -->
          <div class="chat-input-area">
            <el-input
              v-model="chatInput"
              placeholder="请描述您的症状或回答问题..."
              :disabled="chatLoading || chatCompleted"
              @keyup.enter="sendChatMessage"
              size="large"
            >
              <template #append>
                <el-button :loading="chatLoading" :disabled="!chatInput.trim() || chatCompleted" @click="sendChatMessage">
                  <el-icon><Promotion /></el-icon> 发送
                </el-button>
              </template>
            </el-input>
          </div>
        </div>

        <!-- 预约列表 -->
        <div class="section-card list-section">
          <div class="section-header">
            <div class="section-title">
              <el-icon class="section-icon"><List /></el-icon>
              <span>我的预约</span>
            </div>
            <div class="section-tags">
              <el-tag effect="light" type="info" size="small" round>共 {{ store.total }} 条</el-tag>
            </div>
          </div>

          <div class="table-container">
            <el-table :data="store.appointments" v-loading="store.loading" stripe border>
              <el-table-column prop="appointmentDate" label="预约日期" width="110" />
              <el-table-column prop="timeSlot" label="时段" width="120" />
              <el-table-column prop="departmentName" label="科室" width="100" />
              <el-table-column prop="doctorName" label="医生" width="90" />
              <el-table-column prop="appointmentType" label="类型" width="90">
                <template #default="{ row }">
                  <el-tag :type="row.appointmentType === 'DOCTOR' ? '' : 'warning'" size="small" effect="light">
                    {{ row.appointmentType === 'DOCTOR' ? '医生预约' : '检查预约' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="85">
                <template #default="{ row }">
                  <el-tag :type="statusTagType[row.status]" size="small" effect="light">{{ statusMap[row.status] }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="140" fixed="right">
                <template #default="{ row }">
                  <template v-if="row.status === 0">
                    <el-button link type="warning" @click="handleCancel(row)">取消</el-button>
                    <el-button link type="primary" @click="handleReschedule(row)">改期</el-button>
                  </template>
                  <span v-else class="status-text">-</span>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div class="pagination-container">
            <el-pagination
              v-model:current-page="queryParams.page"
              v-model:page-size="queryParams.size"
              :total="store.total"
              layout="total, prev, pager, next"
              background
              @change="loadList"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- 新建预约对话框 -->
    <el-dialog v-model="showCreate" title="新建预约" width="1400px" class="create-dialog" @open="resetCreateForm">
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
              <el-avatar :size="64" :icon="'UserFilled'" />
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
      <el-dialog v-model="showScheduleDialog" title="选择就诊时段" width="1000px" append-to-body class="schedule-dialog">
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
                <el-tag :type="slot.bookedCount >= slot.maxPatients ? 'danger' : 'success'" size="small" effect="light">
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
        <div class="preview-header">
          <el-icon><Check /></el-icon> 已选时段
        </div>
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
        <el-button size="large" @click="showCreate = false">取消</el-button>
        <el-button type="primary" size="large" :loading="createLoading" :disabled="!selectedSlot" @click="handleCreate">
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
import { Calendar, Plus, ChatDotRound, User, Star, Clock, Check, Warning, Promotion, List } from '@element-plus/icons-vue'
import { useAppointmentStore } from '@/stores/appointment'
import { getDepartmentList, getDoctorList } from '@/api/department'
import { getChatHistory, saveChatMessages, clearChatHistory } from '@/api/chatHistory'
import type { Appointment, AppointmentCreateDto, AppointmentRecommendation, ChatMessageDto, Department, Doctor, DoctorSchedule, DoctorWithSchedule } from '@/types'

const CHAT_TYPE = 'TRIAGE'

const store = useAppointmentStore()
const showCreate = ref(false)
const createLoading = ref(false)
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelReason = ref('')
const cancelTarget = ref<Appointment | null>(null)
const recommendation = ref<AppointmentRecommendation | null>(null)
const departments = ref<Department[]>([])
const allDoctors = ref<Doctor[]>([])

// 聊天相关状态
const defaultWelcome = '您好！我是AI导诊助手，请问您今天哪里不舒服？请描述一下您的症状。'
const chatMessages = ref<ChatMessageDto[]>([
  { role: 'assistant', content: defaultWelcome }
])
const chatInput = ref('')
const chatLoading = ref(false)
const chatCompleted = ref(false)
const chatMessagesRef = ref<HTMLElement | null>(null)
const chatHistoryLoaded = ref(false)

// AI推荐后的医生选择状态
const selectedDoctorForBooking = ref<DoctorWithSchedule | null>(null)
const selectedSlotForBooking = ref<DoctorSchedule | null>(null)

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

    // 保存本次对话到后端（用户消息+AI回复）
    saveChatMessages({
      chatType: CHAT_TYPE,
      messages: [
        { role: 'user', content: msg },
        { role: 'assistant', content: res.reply }
      ]
    }).catch(() => {})

    if (res.completed) {
      chatCompleted.value = true
      recommendation.value = res.recommendation || null
      // 如果只有一位医生有排班，自动选中
      if (recommendation.value?.availableDoctors?.length === 1) {
        selectedDoctorForBooking.value = recommendation.value.availableDoctors[0]
      }
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
    { role: 'assistant', content: defaultWelcome }
  ]
  chatInput.value = ''
  chatLoading.value = false
  chatCompleted.value = false
  recommendation.value = null
  selectedDoctorForBooking.value = null
  selectedSlotForBooking.value = null
  // 清除后端聊天历史
  clearChatHistory(CHAT_TYPE).catch(() => {})
}

// 应用筛选条件
async function applyFilter() {
  if (filterDate.value) {
    // 如果选择了日期，查询该日期的排班（同时传科室ID以精确查询）
    await store.fetchSchedules({
      departmentId: filterDepartment.value || undefined,
      date: filterDate.value,
    })

    // 只保留可用的排班
    doctorAvailableSlots.value = store.schedules.filter(s => s.bookedCount < s.maxPatients)
  } else if (filterDepartment.value) {
    // 只选了科室没选日期，查询该科室未来7天的排班
    await store.fetchSchedules({
      departmentId: filterDepartment.value,
    })

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

async function handleAiBook() {
  if (!selectedSlotForBooking.value || !recommendation.value) {
    return
  }

  const createDto: AppointmentCreateDto = {
    departmentId: recommendation.value.departmentId!,
    doctorId: selectedSlotForBooking.value.doctorId,
    appointmentType: 'DOCTOR',
    examinationType: undefined,
    appointmentDate: selectedSlotForBooking.value.scheduleDate,
    timeSlot: selectedSlotForBooking.value.timeSlot,
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

  // 加载聊天历史
  try {
    const historyRes = await getChatHistory(CHAT_TYPE)
    if (historyRes.data && historyRes.data.length > 0) {
      chatMessages.value = historyRes.data
      chatHistoryLoaded.value = true
      scrollToBottom()
    }
  } catch { /* 后端不可用，使用默认欢迎语 */ }

  loadList()
})
</script>

<style scoped>
/* 页面布局 - 满屏 */
.appointment-page {
  --ap-primary: var(--his-primary);
  --ap-accent: #2aa06d;
  --ap-primary-rgb: var(--his-primary-rgb);
  --ap-text: var(--his-text);
  --ap-text-muted: var(--his-text-2);
  --ap-border: rgba(var(--his-primary-rgb), 0.18);
  --ap-surface: rgba(255, 255, 255, 0.86);
  --ap-surface-strong: rgba(255, 255, 255, 0.94);
  --ap-gradient: linear-gradient(135deg, var(--ap-primary) 0%, var(--ap-accent) 100%);
  position: relative;
  min-height: calc(100vh - var(--his-header-height));
  background:
    radial-gradient(1000px 480px at -5% -8%, rgba(var(--his-primary-rgb), 0.2), transparent 64%),
    radial-gradient(980px 460px at 110% -12%, rgba(var(--his-primary-rgb), 0.12), transparent 68%),
    linear-gradient(155deg, #f4fdf8 0%, #eefbf3 48%, #f8fffb 100%);
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  inset: 0;
  overflow: hidden;
  pointer-events: none;
}

.bg-circle {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.32;
  animation: orbFloat 12s ease-in-out infinite;
}

.bg-circle-1 {
  width: 460px;
  height: 460px;
  background: radial-gradient(circle at 30% 30%, rgba(var(--his-primary-rgb), 0.9), rgba(var(--his-primary-rgb), 0));
  top: -120px;
  right: -120px;
  animation-delay: -2s;
}

.bg-circle-2 {
  width: 340px;
  height: 340px;
  background: radial-gradient(circle at 35% 30%, rgba(var(--his-primary-rgb), 0.68), rgba(var(--his-primary-rgb), 0));
  bottom: -90px;
  left: -70px;
  animation-delay: -6s;
}

.bg-circle-3 {
  width: 240px;
  height: 240px;
  background: radial-gradient(circle at 35% 30%, rgba(var(--his-primary-rgb), 0.54), rgba(var(--his-primary-rgb), 0));
  top: 45%;
  left: 28%;
  animation-delay: -9s;
}

/* 内容区域 */
.page-content {
  position: relative;
  z-index: 1;
  padding: 22px;
  min-height: calc(100vh - var(--his-header-height));
  display: flex;
  flex-direction: column;
}

/* 页面头部 */
.hero-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
  padding: 24px 30px;
  background: rgba(255, 255, 255, 0.78);
  backdrop-filter: blur(16px) saturate(130%);
  border-radius: 22px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.24);
  box-shadow:
    0 16px 34px rgba(18, 56, 38, 0.1),
    0 8px 18px rgba(var(--his-primary-rgb), 0.1);
}

.hero-left {
  min-width: 0;
}

.hero-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-icon {
  font-size: 30px;
  color: var(--his-primary);
}

.hero-title {
  font-size: 42px;
  line-height: 1.08;
  font-weight: 900;
  letter-spacing: 0.5px;
  color: var(--his-text);
}

.hero-subtitle {
  font-size: 14px;
  color: var(--his-text-2);
  margin: 6px 0 0;
  padding-left: 42px;
}

.hero-action-btn {
  padding: 12px 26px;
  font-size: 15px;
  border-radius: 14px;
  border: none;
  background: var(--ap-gradient);
  box-shadow: 0 12px 24px rgba(var(--his-primary-rgb), 0.3);
}

/* 主网格布局 */
.main-grid {
  display: grid;
  grid-template-columns: 1.05fr 1.35fr;
  gap: 24px;
  flex: 1;
  min-height: 0;
}

/* 卡片通用样式 */
.section-card {
  background: var(--ap-surface);
  backdrop-filter: blur(16px) saturate(130%);
  border-radius: 20px;
  border: 1px solid var(--ap-border);
  box-shadow:
    0 16px 36px rgba(18, 56, 38, 0.08),
    0 8px 22px rgba(var(--his-primary-rgb), 0.08);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: transform 0.24s ease, box-shadow 0.24s ease;
}

.section-card:hover {
  transform: translateY(-2px);
  box-shadow:
    0 22px 44px rgba(18, 56, 38, 0.1),
    0 12px 24px rgba(var(--his-primary-rgb), 0.12);
}

/* 区块头部 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.15), rgba(var(--his-primary-rgb), 0.04));
  border-bottom: 1px solid rgba(var(--his-primary-rgb), 0.14);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--ap-text);
  display: flex;
  align-items: center;
  gap: 10px;
}

.section-icon {
  font-size: 22px;
  color: var(--ap-primary);
}

.section-tags {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* AI导诊区域 */
.ai-section {
  min-height: 0;
  height: 100%;
}

.chat-container {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  min-height: 300px;
}

/* 聊天消息 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 8px 4px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;
}

.chat-messages::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: rgba(var(--his-primary-rgb), 0.28);
  border-radius: 3px;
}

.chat-message {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.chat-message.user {
  flex-direction: row-reverse;
}

.chat-avatar {
  flex-shrink: 0;
}

.avatar-ai {
  background: var(--ap-gradient) !important;
}

.avatar-user {
  background: linear-gradient(135deg, #6dd8a8 0%, #47c88a 100%) !important;
}

.chat-bubble {
  max-width: 78%;
}

.chat-content {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.7;
  word-break: break-word;
  white-space: pre-wrap;
}

.chat-message.assistant .chat-content {
  background: rgba(var(--his-primary-rgb), 0.08);
  color: var(--ap-text);
  border-bottom-left-radius: 4px;
}

.chat-message.user .chat-content {
  background: var(--ap-gradient);
  color: white;
  border-bottom-right-radius: 4px;
}

/* AI正在输入动画 */
.chat-typing {
  display: flex;
  gap: 5px;
  padding: 12px 16px;
  background: rgba(var(--his-primary-rgb), 0.08);
  border-radius: 16px;
  border-bottom-left-radius: 4px;
}

.chat-typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--ap-primary);
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

/* 推荐结果 */
.ai-result {
  flex-shrink: 0;
  overflow: auto;
  padding: 0 20px 16px;
  max-height: 380px;
}

.ai-result :deep(.el-divider) {
  margin: 16px 0;
}

.ai-result :deep(.el-divider__text) {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--ap-text-muted);
}

.recommend-desc {
  margin-bottom: 8px;
}

/* 医生选择卡片 */
.doctor-select-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doctor-select-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.16);
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.8);
}

.doctor-select-card:hover {
  border-color: rgba(var(--his-primary-rgb), 0.42);
  background: rgba(var(--his-primary-rgb), 0.08);
  transform: translateX(4px);
}

.doctor-select-card.selected {
  border-color: var(--ap-primary);
  background: var(--ap-gradient);
  color: white;
}

.doctor-select-info {
  flex: 1;
}

.doctor-select-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
}

.doctor-select-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.doctor-select-card.selected .doctor-select-meta :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: transparent !important;
  color: white !important;
}

.doctor-select-specialty {
  font-size: 12px;
  color: var(--ap-text-muted);
}

.doctor-select-card.selected .doctor-select-specialty {
  color: rgba(255, 255, 255, 0.85);
}

/* 排班列表 */
.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.schedule-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.16);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.82);
}

.schedule-item:hover {
  border-color: rgba(var(--his-primary-rgb), 0.42);
  background: rgba(var(--his-primary-rgb), 0.08);
  transform: translateX(4px);
}

.schedule-item.selected {
  border-color: var(--ap-primary);
  background: var(--ap-gradient);
  color: white;
}

.schedule-date {
  font-weight: 500;
  font-size: 14px;
  min-width: 100px;
}

.schedule-item.selected .schedule-date {
  color: white;
}

.schedule-time {
  font-weight: 600;
  font-size: 15px;
  flex: 1;
}

.schedule-item.selected .schedule-time {
  color: white;
}

.schedule-info {
  white-space: nowrap;
}

.schedule-item.selected .schedule-info :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: transparent !important;
}

.confirm-btn {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 15px;
}

.no-schedule {
  padding: 24px;
  text-align: center;
}

/* 输入框区域 */
.chat-input-area {
  padding: 16px 20px 20px;
  border-top: 1px solid rgba(var(--his-primary-rgb), 0.14);
  background: rgba(var(--his-primary-rgb), 0.03);
}

.chat-input-area :deep(.el-input__wrapper) {
  border-radius: 12px;
  box-shadow: 0 2px 10px rgba(var(--his-primary-rgb), 0.14);
}

.chat-input-area :deep(.el-input-group__append) {
  border-radius: 0 12px 12px 0;
  padding: 0 16px;
}

/* 预约列表区域 */
.list-section {
  flex: 1;
  min-height: 0;
  height: 100%;
}

.table-container {
  flex: 1;
  overflow: auto;
  padding: 16px 20px;
}

.table-container :deep(.el-table) {
  border-radius: 12px;
  overflow: hidden;
}

.table-container :deep(.el-table th) {
  background: rgba(var(--his-primary-rgb), 0.1) !important;
  color: var(--ap-text-muted);
  font-weight: 600;
}

.table-container :deep(.el-table__row) {
  transition: background 0.2s;
}

.table-container :deep(.el-table__row:hover > td) {
  background: rgba(var(--his-primary-rgb), 0.08) !important;
}

.status-text {
  color: #c0c4cc;
}

.pagination-container {
  padding: 16px 20px 20px;
  border-top: 1px solid rgba(var(--his-primary-rgb), 0.14);
  display: flex;
  justify-content: flex-end;
}

/* 筛选工具栏 */
.filter-toolbar {
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.1), rgba(var(--his-primary-rgb), 0.03));
  border-radius: 16px;
  margin-bottom: 20px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.18);
}

.filter-form {
  margin: 0;
}

/* 医生表格 */
.doctor-table-container {
  max-height: 420px;
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
  padding: 20px 24px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.16);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  background: rgba(255, 255, 255, 0.84);
}

.doctor-row:hover {
  border-color: rgba(var(--his-primary-rgb), 0.38);
  box-shadow: 0 8px 24px rgba(var(--his-primary-rgb), 0.18);
  transform: translateY(-2px);
}

.doctor-row.selected {
  border-color: var(--ap-primary);
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.15), rgba(var(--his-primary-rgb), 0.06));
}

.doctor-avatar {
  flex-shrink: 0;
}

.doctor-info-section {
  flex: 1;
}

.doctor-row .doctor-name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--ap-text);
}

.doctor-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
  font-size: 14px;
}

.doctor-dept {
  color: var(--ap-text-muted);
}

.doctor-gender,
.doctor-age {
  color: var(--ap-text-muted);
}

.doctor-specialty {
  font-size: 13px;
  color: var(--ap-text-muted);
  line-height: 1.5;
}

.doctor-action-section {
  flex-shrink: 0;
}

.empty-state {
  padding: 48px 20px;
  text-align: center;
}

/* 排班弹窗 */
.schedule-dialog-content {
  padding: 16px;
}

.schedule-doctor-info {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: var(--ap-gradient);
  border-radius: 16px;
  color: white;
  margin-bottom: 20px;
}

.doctor-detail-name {
  font-size: 22px;
  font-weight: 600;
  margin-bottom: 8px;
}

.doctor-detail-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.schedule-doctor-info :deep(.el-tag) {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: transparent !important;
  color: white !important;
}

.schedule-slots {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 14px;
  max-height: 380px;
  overflow-y: auto;
}

.schedule-slot-card {
  border: 1px solid rgba(var(--his-primary-rgb), 0.18);
  border-radius: 14px;
  padding: 18px;
  cursor: pointer;
  transition: all 0.2s ease;
  background: rgba(255, 255, 255, 0.82);
}

.schedule-slot-card:hover {
  border-color: rgba(var(--his-primary-rgb), 0.42);
  box-shadow: 0 6px 18px rgba(var(--his-primary-rgb), 0.18);
  transform: translateY(-2px);
}

.schedule-slot-card.selected {
  border-color: var(--ap-primary);
  background: var(--ap-gradient);
  color: white;
}

.slot-header {
  margin-bottom: 12px;
}

.slot-date {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
}

.schedule-slot-card.selected .slot-date {
  color: white;
}

.slot-time {
  font-size: 14px;
  color: var(--ap-text-muted);
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
  color: var(--ap-text-muted);
}

.schedule-slot-card.selected .slot-count {
  color: rgba(255, 255, 255, 0.85);
}

/* 已选时段预览 */
.selected-preview {
  margin-top: 20px;
  padding: 20px 24px;
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.11), rgba(var(--his-primary-rgb), 0.03));
  border-radius: 16px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.2);
}

.preview-header {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
  color: var(--ap-primary);
  display: flex;
  align-items: center;
  gap: 8px;
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
  color: var(--ap-text-muted);
}

.preview-value {
  color: var(--ap-text);
  font-weight: 600;
}

/* 弹窗统一美化 */
.appointment-page :deep(.el-dialog) {
  border-radius: 18px;
  border: 1px solid var(--ap-border);
  box-shadow:
    0 26px 56px rgba(18, 56, 38, 0.14),
    0 12px 24px rgba(var(--his-primary-rgb), 0.12);
  overflow: hidden;
}

.appointment-page :deep(.el-dialog__header) {
  margin-bottom: 0;
  padding: 16px 20px 12px;
  background: linear-gradient(180deg, rgba(var(--his-primary-rgb), 0.14), rgba(255, 255, 255, 0.03));
}

.appointment-page :deep(.el-dialog__body) {
  padding-top: 14px;
}

/* 背景动态 */
@keyframes orbFloat {
  0%, 100% { transform: translate3d(0, 0, 0); }
  50% { transform: translate3d(14px, -12px, 0); }
}

/* 响应式适配 */
@media (max-width: 1200px) {
  .main-grid {
    grid-template-columns: 1fr;
  }

  .hero-header {
    padding: 16px 14px;
    border-radius: 16px;
  }

  .hero-icon {
    font-size: 24px;
  }

  .hero-title {
    font-size: 28px;
  }

  .hero-subtitle {
    padding-left: 36px;
    font-size: 12px;
  }
}

@media (max-width: 768px) {
  .page-content {
    padding: 16px;
  }

  .hero-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .hero-title {
    font-size: 24px;
  }

  .hero-subtitle {
    padding-left: 36px;
    font-size: 12px;
  }

  .hero-action-btn {
    padding: 10px 20px;
    border-radius: 12px;
  }
}
</style>
