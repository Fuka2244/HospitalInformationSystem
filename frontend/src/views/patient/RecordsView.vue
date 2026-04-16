<template>
  <div class="records-container" style="padding: 20px">
    <el-card shadow="hover">
      <template #header>
        <div class="header-row">
          <span>病历与就诊记录</span>
          <div>
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
import { getVisitHistory, getMedicalRecordDetail } from '@/api/patient'
import { getDepartmentList, getDepartmentDoctors } from '@/api/department'
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
  const deptRes = await getDepartmentList()
  departments.value = deptRes.data || []
  loadRecords()
})
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
}
</style>
