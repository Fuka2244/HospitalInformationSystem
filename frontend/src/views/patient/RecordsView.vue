<template>
  <div class="page records-container">
    <div class="page-header hero-header">
      <div class="hero-left">
        <div class="hero-title-row">
          <el-icon class="hero-icon"><Calendar /></el-icon>
          <span class="hero-title">病历与就诊</span>
        </div>
        <div class="hero-subtitle">按科室/医生/日期快速筛选与查看病历详情</div>
      </div>
    </div>
    <el-card class="records-card" shadow="hover">
      <template #header>
        <div class="header-row">
          <div class="head-left">
            <span>就诊记录</span>
            <el-tag effect="light" type="info" size="small">共 {{ total }} 条</el-tag>
          </div>
          <div class="filters">
            <el-select v-model="query.departmentId" placeholder="按科室筛选" clearable style="width: 160px; margin-right: 8px">
              <el-option v-for="d in departments" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
            <el-select v-model="query.doctorId" placeholder="按医生筛选" clearable style="width: 160px; margin-right: 8px">
              <el-option v-for="d in doctors" :key="d.id" :label="d.name" :value="d.id" />
            </el-select>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="margin-right: 8px" />
            <el-button type="primary" @click="handleSearch">查询</el-button>
          </div>
        </div>
      </template>

      <el-table :data="records" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="visitDate" label="就诊日期" width="180" />
        <el-table-column prop="departmentName" label="科室" width="120" />
        <el-table-column prop="doctorName" label="医生" width="100" />
        <el-table-column prop="diagnosis" label="诊断" min-width="150" />
        <el-table-column prop="prescriptionSummary" label="处方摘要" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row.recordId)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @change="loadRecords"
      />
    </el-card>

    <!-- 病历详情对话框 -->
    <el-dialog v-model="detailVisible" title="病历详情" width="700px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions title="病历信息" :column="2" border>
            <el-descriptions-item label="患者">{{ detail.record.patientName }}</el-descriptions-item>
            <el-descriptions-item label="医生">{{ detail.record.doctorName }}</el-descriptions-item>
            <el-descriptions-item label="科室">{{ detail.record.departmentName }}</el-descriptions-item>
            <el-descriptions-item label="就诊日期">{{ detail.record.visitDate }}</el-descriptions-item>
            <el-descriptions-item label="主诉" :span="2">{{ detail.record.chiefComplaint }}</el-descriptions-item>
            <el-descriptions-item label="现病史" :span="2">{{ detail.record.presentIllness }}</el-descriptions-item>
            <el-descriptions-item label="诊断" :span="2">{{ detail.record.diagnosis }}</el-descriptions-item>
            <el-descriptions-item label="治疗方案" :span="2">{{ detail.record.treatmentPlan }}</el-descriptions-item>
          </el-descriptions>

          <el-divider>处方信息</el-divider>
          <template v-if="detail.prescription">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="处方日期">{{ detail.prescription.prescriptionDate }}</el-descriptions-item>
              <el-descriptions-item label="状态">{{ prescriptionStatusMap[detail.prescription.status] }}</el-descriptions-item>
            </el-descriptions>
            <el-table :data="detail.prescriptionItems" border size="small" style="margin-top: 12px">
              <el-table-column prop="medicineName" label="药品" />
              <el-table-column prop="dosage" label="用法用量" />
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column prop="days" label="天数" width="80" />
            </el-table>
          </template>
          <el-empty v-else description="暂无处方" />
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar } from '@element-plus/icons-vue'
import { getVisitHistory, getMedicalRecordDetail } from '@/api/patient'
import { getDepartmentList, getDoctorList } from '@/api/department'
import type { VisitRecord, VisitHistoryParams, MedicalRecordDetail, Department, Doctor } from '@/types'

const loading = ref(false)
const records = ref<VisitRecord[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<MedicalRecordDetail | null>(null)
const departments = ref<Department[]>([])
const doctors = ref<Doctor[]>([])
const dateRange = ref<[string, string] | null>(null)

const prescriptionStatusMap: Record<number, string> = { 0: '待取药', 1: '已取药', 2: '已取消' }

const query = reactive<VisitHistoryParams>({ page: 1, size: 10 })

async function loadRecords() {
  loading.value = true
  try {
    if (dateRange.value) {
      query.startDate = dateRange.value[0]
      query.endDate = dateRange.value[1]
    } else {
      query.startDate = undefined
      query.endDate = undefined
    }
    const res = await getVisitHistory(query)
    records.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function showDetail(recordId: number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getMedicalRecordDetail(recordId)
    detail.value = res.data
  } catch {
    detail.value = null
  } finally {
    detailLoading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadRecords()
}

onMounted(async () => {
  try {
    const deptRes = await getDepartmentList()
    departments.value = deptRes.data || []
  } catch { /* 后端不可用 */ }

  try {
    const doctorRes = await getDoctorList()
    doctors.value = doctorRes.data || []
  } catch { /* 后端不可用 */ }

  loadRecords()
})
</script>

<style scoped>
.records-container {
  position: relative;
  min-height: calc(100vh - var(--his-header-height));
  background:
    radial-gradient(860px 420px at -6% -8%, rgba(var(--his-primary-rgb), 0.18), transparent 64%),
    radial-gradient(760px 360px at 108% 0%, rgba(var(--his-primary-rgb), 0.12), transparent 68%),
    linear-gradient(160deg, #f5fdf8 0%, #eefaf3 52%, #f8fffb 100%);
  overflow: hidden;
}

.records-container::before,
.records-container::after {
  content: "";
  position: absolute;
  border-radius: 999px;
  pointer-events: none;
  filter: blur(2px);
}

.records-container::before {
  width: 420px;
  height: 420px;
  top: -160px;
  left: -140px;
  background: radial-gradient(circle at 30% 30%, rgba(var(--his-primary-rgb), 0.22), rgba(var(--his-primary-rgb), 0));
}

.records-container::after {
  width: 540px;
  height: 540px;
  right: -240px;
  bottom: -240px;
  background: radial-gradient(circle at 35% 30%, rgba(var(--his-primary-rgb), 0.16), rgba(var(--his-primary-rgb), 0));
}

.records-container .page-header,
.records-card {
  position: relative;
  z-index: 1;
}

.records-container .page-header {
  margin-bottom: 16px;
}

.hero-header {
  padding: 22px 28px;
  border-radius: 22px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.24);
  background: rgba(255, 255, 255, 0.76);
  backdrop-filter: blur(12px) saturate(130%);
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
  margin-top: 6px;
  padding-left: 42px;
  font-size: 14px;
  color: var(--his-text-2);
}

.records-card {
  min-height: calc(100vh - var(--his-header-height) - 122px);
  border: 1px solid rgba(var(--his-primary-rgb), 0.2);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px) saturate(130%);
  box-shadow:
    0 18px 40px rgba(18, 56, 38, 0.1),
    0 10px 22px rgba(var(--his-primary-rgb), 0.08);
}

.records-card :deep(.el-card__header) {
  border-bottom: 1px solid rgba(var(--his-primary-rgb), 0.14);
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.12), rgba(var(--his-primary-rgb), 0.04));
}

.records-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.records-card :deep(.el-table) {
  flex: 1;
  border-radius: 14px;
  overflow: hidden;
}

.records-card :deep(.el-table th.el-table__cell) {
  background: rgba(var(--his-primary-rgb), 0.1);
}

.records-card :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(var(--his-primary-rgb), 0.08) !important;
}

.records-card :deep(.el-pagination) {
  margin-top: auto;
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.head-left {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 800;
  letter-spacing: 0.2px;
  color: var(--his-text);
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.filters :deep(.el-input__wrapper),
.filters :deep(.el-select__wrapper),
.filters :deep(.el-range-editor.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(var(--his-primary-rgb), 0.2);
  box-shadow: none;
}

.filters :deep(.el-input__wrapper.is-focus),
.filters :deep(.el-select__wrapper.is-focused),
.filters :deep(.el-range-editor.el-input__wrapper.is-focus) {
  border-color: rgba(var(--his-primary-rgb), 0.44);
  box-shadow: 0 0 0 1px rgba(var(--his-primary-rgb), 0.18) inset;
}

.records-container :deep(.el-dialog) {
  border-radius: 16px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.2);
  overflow: hidden;
}

.records-container :deep(.el-dialog__header) {
  margin-bottom: 0;
  background: linear-gradient(180deg, rgba(var(--his-primary-rgb), 0.12), rgba(255, 255, 255, 0.02));
}

@media (max-width: 768px) {
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

  .records-card {
    min-height: auto;
  }
}
</style>
