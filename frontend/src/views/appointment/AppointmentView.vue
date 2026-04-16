<template>
  <div class="appointment-container" style="padding: 20px">
    <el-row :gutter="20">
      <!-- AI智能导诊 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>AI 智能导诊</span>
          </template>
          <el-input v-model="symptom" type="textarea" :rows="3" placeholder="请描述您的症状，如：我最近头痛，应该挂什么科？" />
          <el-button type="primary" :loading="aiLoading" style="width: 100%; margin-top: 12px" @click="handleAiRecommend">
            AI 智能推荐
          </el-button>
          <div v-if="recommendation" class="ai-result">
            <el-divider>推荐结果</el-divider>
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="推荐科室">
                <el-tag type="success">{{ recommendation.department }}</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="推荐医生">{{ recommendation.doctor }}</el-descriptions-item>
              <el-descriptions-item label="推荐时段">{{ recommendation.recommendedTime }}</el-descriptions-item>
              <el-descriptions-item label="推荐理由">{{ recommendation.reason }}</el-descriptions-item>
            </el-descriptions>
          </div>
        </el-card>
      </el-col>

      <!-- 预约列表 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="header-row">
              <span>我的预约</span>
              <el-button type="primary" @click="showCreate = true">新建预约</el-button>
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
    <el-dialog v-model="showCreate" title="新建预约" width="550px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="科室" prop="departmentId">
          <el-select v-model="createForm.departmentId" placeholder="请选择科室" @change="onDeptChange">
            <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="医生" prop="doctorId">
          <el-select v-model="createForm.doctorId" placeholder="请选择医生">
            <el-option v-for="d in deptDoctors" :key="d.id" :label="`${d.name} - ${d.title}`" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预约类型" prop="appointmentType">
          <el-radio-group v-model="createForm.appointmentType">
            <el-radio value="DOCTOR">医生预约</el-radio>
            <el-radio value="EXAMINATION">检查预约</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="createForm.appointmentType === 'EXAMINATION'" label="检查类型" prop="examinationType">
          <el-input v-model="createForm.examinationType" placeholder="如：CT、B超等" />
        </el-form-item>
        <el-form-item label="预约日期" prop="appointmentDate">
          <el-date-picker v-model="createForm.appointmentDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" :disabled-date="disablePastDate" />
        </el-form-item>
        <el-form-item label="时段" prop="timeSlot">
          <el-select v-model="createForm.timeSlot" placeholder="选择时段" @focus="loadSchedules">
            <el-option v-for="s in availableSlots" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">确认预约</el-button>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAppointmentStore } from '@/stores/appointment'
import { getDepartmentList, getDepartmentDoctors } from '@/api/department'
import type { Appointment, AppointmentCreateDto, AppointmentRecommendation, Department, Doctor } from '@/types'

const store = useAppointmentStore()
const showCreate = ref(false)
const createLoading = ref(false)
const aiLoading = ref(false)
const cancelVisible = ref(false)
const cancelLoading = ref(false)
const cancelReason = ref('')
const cancelTarget = ref<Appointment | null>(null)
const symptom = ref('')
const recommendation = ref<AppointmentRecommendation | null>(null)
const departments = ref<Department[]>([])
const deptDoctors = ref<Doctor[]>([])
const createFormRef = ref<FormInstance>()

const statusMap: Record<number, string> = { 0: '已预约', 1: '已完成', 2: '已取消' }
const statusTagType: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }

const createForm = reactive<AppointmentCreateDto>({
  departmentId: 0,
  doctorId: undefined,
  appointmentType: 'DOCTOR',
  examinationType: undefined,
  appointmentDate: '',
  timeSlot: '',
})

const createRules: FormRules<AppointmentCreateDto> = {
  departmentId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  appointmentType: [{ required: true, message: '请选择预约类型', trigger: 'change' }],
  appointmentDate: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  timeSlot: [{ required: true, message: '请选择时段', trigger: 'change' }],
}

const queryParams = reactive({ page: 1, size: 10, status: undefined as number | undefined })

const availableSlots = computed(() => {
  return store.schedules
    .filter(s => {
      if (createForm.doctorId && s.doctorId !== createForm.doctorId) return false
      if (createForm.appointmentDate && s.scheduleDate !== createForm.appointmentDate) return false
      return s.bookedCount < s.maxPatients
    })
    .map(s => s.timeSlot)
})

function disablePastDate(date: Date) {
  return date.getTime() < Date.now() - 86400000
}

async function onDeptChange(deptId: number) {
  createForm.doctorId = undefined
  const res = await getDepartmentDoctors(deptId)
  deptDoctors.value = res.data || []
}

async function loadSchedules() {
  if (createForm.departmentId || createForm.doctorId || createForm.appointmentDate) {
    await store.fetchSchedules({
      departmentId: createForm.departmentId || undefined,
      doctorId: createForm.doctorId || undefined,
      date: createForm.appointmentDate || undefined,
    })
  }
}

async function handleAiRecommend() {
  if (!symptom.value.trim()) {
    ElMessage.warning('请描述您的症状')
    return
  }
  aiLoading.value = true
  try {
    const res = await store.aiRecommend(symptom.value)
    recommendation.value = res.data
  } finally {
    aiLoading.value = false
  }
}

async function handleCreate() {
  await createFormRef.value?.validate()
  createLoading.value = true
  try {
    await store.create(createForm)
    ElMessage.success('预约成功')
    showCreate.value = false
    loadList()
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
    createForm.departmentId = row.departmentId
    createForm.doctorId = row.doctorId ?? undefined
    createForm.appointmentType = row.appointmentType as any
    createForm.appointmentDate = ''
    createForm.timeSlot = ''
    showCreate.value = true
  } catch { /* 用户取消 */ }
}

async function loadList() {
  await store.fetchList(queryParams)
}

onMounted(async () => {
  const deptRes = await getDepartmentList()
  departments.value = deptRes.data || []
  loadList()
})
</script>

<style scoped>
.header-row { display: flex; justify-content: space-between; align-items: center; }
.ai-result { margin-top: 8px; }
</style>
