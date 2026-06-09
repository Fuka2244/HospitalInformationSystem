<template>
  <div class="staff-page">
    <section class="staff-header">
      <div>
        <div class="eyebrow">Doctor Workstation</div>
        <h1>医生工作台</h1>
        <p>查看今日预约，完成叫号、接诊、结束就诊和患者病历追踪。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </section>

    <el-row :gutter="16" class="metric-row">
      <el-col v-for="item in metrics" :key="item.label" :xs="12" :md="6">
        <el-card shadow="never" class="metric-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="work-card" shadow="never">
      <template #header>
        <div class="card-head">
          <strong>今日接诊队列</strong>
          <el-segmented v-model="statusFilter" :options="statusOptions" />
        </div>
      </template>

      <el-table :data="filteredAppointments" v-loading="loading" stripe>
        <el-table-column prop="timeSlot" label="时段" width="130" />
        <el-table-column prop="patientName" label="患者" min-width="120">
          <template #default="{ row }">{{ row.patientName || row.patientId }}</template>
        </el-table-column>
        <el-table-column prop="departmentName" label="科室" min-width="120" />
        <el-table-column prop="appointmentType" label="类型" width="110" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="appointmentStatus[row.status]?.type || 'info'">
              {{ appointmentStatus[row.status]?.text || `状态 ${row.status}` }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRecords(row)">病历</el-button>
            <el-button link type="success" :disabled="row.status !== 0" @click="openCall(row)">叫号</el-button>
            <el-button link type="warning" :disabled="row.status !== 1" @click="handleStartVisit(row.id)">接诊</el-button>
            <el-button link type="danger" :disabled="row.status !== 2" @click="openEndVisit(row)">结束</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="callVisible" title="叫号患者" width="420px">
      <el-form label-width="82px">
        <el-form-item label="患者">{{ currentAppointment?.patientName || currentAppointment?.patientId }}</el-form-item>
        <el-form-item label="诊室位置">
          <el-input v-model="callLocation" placeholder="例如：门诊二楼 203 诊室" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="callVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleCallPatient">确认叫号</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="endVisible" title="结束就诊" width="620px">
      <el-form label-position="top">
        <el-form-item label="诊断结果">
          <el-input v-model="visitForm.diagnosis" type="textarea" :rows="3" placeholder="填写诊断结果" />
        </el-form-item>
        <el-form-item label="治疗方案">
          <el-input v-model="visitForm.treatment" type="textarea" :rows="3" placeholder="填写治疗方案或医嘱" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="visitForm.notes" type="textarea" :rows="2" placeholder="补充说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="endVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleEndVisit">保存并结束</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="recordsVisible" title="患者历史病历" width="860px">
      <el-table :data="records" v-loading="recordsLoading" stripe>
        <el-table-column prop="visitDate" label="就诊时间" width="170" />
        <el-table-column prop="departmentName" label="科室" width="110" />
        <el-table-column prop="doctorName" label="医生" width="110" />
        <el-table-column prop="diagnosis" label="诊断" min-width="180" show-overflow-tooltip />
        <el-table-column prop="treatmentPlan" label="治疗方案" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { callPatient, endVisit, getPatientMedicalRecords, getTodayAppointments, startVisit } from '@/api/doctor'
import type { Appointment, MedicalRecord } from '@/types'

const loading = ref(false)
const actionLoading = ref(false)
const recordsLoading = ref(false)
const appointments = ref<Appointment[]>([])
const records = ref<MedicalRecord[]>([])
const statusFilter = ref('全部')
const callVisible = ref(false)
const endVisible = ref(false)
const recordsVisible = ref(false)
const callLocation = ref('门诊二楼 203 诊室')
const currentAppointment = ref<Appointment | null>(null)
const visitForm = reactive({ diagnosis: '', treatment: '', notes: '' })

const statusOptions = ['全部', '待叫号', '已叫号', '接诊中', '已完成']
const appointmentStatus: Record<number, { text: string; type: '' | 'success' | 'warning' | 'info' | 'danger' }> = {
  0: { text: '待叫号', type: 'info' },
  1: { text: '已叫号', type: 'warning' },
  2: { text: '接诊中', type: '' },
  3: { text: '已完成', type: 'success' },
  [-1]: { text: '已取消', type: 'danger' },
}

const filteredAppointments = computed(() => {
  const statusMap: Record<string, number | null> = { 全部: null, 待叫号: 0, 已叫号: 1, 接诊中: 2, 已完成: 3 }
  const target = statusMap[statusFilter.value]
  return target === null ? appointments.value : appointments.value.filter((item) => item.status === target)
})

const metrics = computed(() => [
  { label: '今日预约', value: appointments.value.length },
  { label: '待叫号', value: appointments.value.filter((item) => item.status === 0).length },
  { label: '接诊中', value: appointments.value.filter((item) => item.status === 2).length },
  { label: '已完成', value: appointments.value.filter((item) => item.status === 3).length },
])

async function loadData() {
  loading.value = true
  try {
    const res = await getTodayAppointments()
    appointments.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openCall(row: Appointment) {
  currentAppointment.value = row
  callVisible.value = true
}

async function handleCallPatient() {
  if (!currentAppointment.value) return
  actionLoading.value = true
  try {
    await callPatient({ appointmentId: currentAppointment.value.id, location: callLocation.value })
    ElMessage.success('叫号成功')
    callVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}

async function handleStartVisit(id: number) {
  actionLoading.value = true
  try {
    await startVisit(id)
    ElMessage.success('已开始接诊')
    loadData()
  } finally {
    actionLoading.value = false
  }
}

function openEndVisit(row: Appointment) {
  currentAppointment.value = row
  Object.assign(visitForm, { diagnosis: '', treatment: '', notes: '' })
  endVisible.value = true
}

async function handleEndVisit() {
  if (!currentAppointment.value) return
  actionLoading.value = true
  try {
    await endVisit(currentAppointment.value.id, { appointmentId: currentAppointment.value.id, ...visitForm })
    ElMessage.success('就诊已结束')
    endVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}

async function openRecords(row: Appointment) {
  recordsVisible.value = true
  recordsLoading.value = true
  try {
    const res = await getPatientMedicalRecords(row.patientId)
    records.value = res.data || []
  } finally {
    recordsLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.staff-page { min-height: calc(100vh - var(--his-header-height)); padding: 18px; background: linear-gradient(160deg, #f6fff9 0%, #eefaf4 52%, #ffffff 100%); }
.staff-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 16px; padding: 22px 26px; border: 1px solid rgba(var(--his-primary-rgb), .18); border-radius: 8px; background: rgba(255,255,255,.86); }
.eyebrow { color: var(--his-primary); font-size: 12px; font-weight: 800; text-transform: uppercase; }
h1 { margin: 4px 0 6px; font-size: 30px; line-height: 1.1; color: var(--his-text); }
p { margin: 0; color: var(--his-text-2); }
.metric-row { margin-bottom: 16px; }
.metric-card { border-radius: 8px; }
.metric-card :deep(.el-card__body) { display: flex; flex-direction: column; gap: 8px; }
.metric-card span { color: var(--his-text-2); font-size: 13px; }
.metric-card strong { font-size: 28px; color: var(--his-text); }
.work-card { border-radius: 8px; }
.card-head { display: flex; align-items: center; justify-content: space-between; gap: 14px; flex-wrap: wrap; }
@media (max-width: 768px) { .staff-header { align-items: flex-start; flex-direction: column; } }
</style>
