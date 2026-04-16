<template>
  <div class="report-container" style="padding: 20px">
    <el-row :gutter="20">
      <!-- 报告列表 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="header-row">
              <span>医疗报告</span>
              <div>
                <el-select v-model="queryParams.reportType" placeholder="报告类型" clearable style="width: 140px; margin-right: 8px">
                  <el-option label="检查报告" value="EXAMINATION" />
                  <el-option label="治疗报告" value="TREATMENT" />
                </el-select>
                <el-button type="primary" @click="showGenerate = true">AI 生成报告</el-button>
              </div>
            </div>
          </template>

          <el-table :data="reports" v-loading="loading" stripe>
            <el-table-column prop="title" label="报告标题" min-width="180" />
            <el-table-column prop="reportType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.reportType === 'EXAMINATION' ? '' : 'success'" size="small">
                  {{ row.reportType === 'EXAMINATION' ? '检查报告' : '治疗报告' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="reportStatusType[row.status]" size="small">{{ reportStatusMap[row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="生成时间" width="170" />
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="showDetail(row.id)">查看</el-button>
                <el-button link type="success" @click="handleExportPdf(row.id)">导出PDF</el-button>
                <el-button v-if="row.status === 0" link type="warning" @click="handleConfirm(row.id)">确认</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            layout="total, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @change="loadReports"
          />
        </el-card>
      </el-col>

      <!-- 快速信息面板 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>报告生成说明</span>
          </template>
          <el-timeline>
            <el-timeline-item type="primary">步骤1：AI分析主诉和现病史</el-timeline-item>
            <el-timeline-item type="primary">步骤2：结合检查结果分析疾病</el-timeline-item>
            <el-timeline-item type="primary">步骤3：参考历史记录判断慢性病</el-timeline-item>
            <el-timeline-item type="primary">步骤4：综合分析给出诊断</el-timeline-item>
            <el-timeline-item type="primary">步骤5：提出治疗方案和建议</el-timeline-item>
          </el-timeline>
          <el-alert type="warning" :closable="false" description="AI生成的报告为草稿状态，需确认后生效。报告内容仅供参考，不作为正式医疗依据。" />
        </el-card>
      </el-col>
    </el-row>

    <!-- AI生成报告对话框 -->
    <el-dialog v-model="showGenerate" title="AI 生成医疗报告" width="550px">
      <el-form ref="genFormRef" :model="genForm" :rules="genRules" label-width="100px">
        <el-form-item label="报告类型" prop="reportType">
          <el-radio-group v-model="genForm.reportType">
            <el-radio value="EXAMINATION">检查报告</el-radio>
            <el-radio value="TREATMENT">治疗报告</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="报告标题" prop="title">
          <el-input v-model="genForm.title" placeholder="如：头部检查报告" />
        </el-form-item>
        <el-form-item label="病历ID">
          <el-input-number v-model="genForm.medicalRecordId" :min="0" placeholder="可选，留空使用最近病历" />
        </el-form-item>
        <el-form-item label="检查数据" prop="examinationData">
          <el-input v-model="genForm.examinationData" type="textarea" :rows="4" placeholder="如：头部CT: 未见明显异常; 血常规: 白细胞计数偏高" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGenerate = false">取消</el-button>
        <el-button type="primary" :loading="genLoading" @click="handleGenerate">生成报告</el-button>
      </template>
    </el-dialog>

    <!-- 报告详情对话框 -->
    <el-dialog v-model="detailVisible" title="报告详情" width="700px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="报告标题" :span="2">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
            <el-descriptions-item label="医生">{{ detail.doctorName }}</el-descriptions-item>
            <el-descriptions-item label="报告类型">{{ detail.reportType === 'EXAMINATION' ? '检查报告' : '治疗报告' }}</el-descriptions-item>
            <el-descriptions-item label="状态">{{ reportStatusMap[detail.status] }}</el-descriptions-item>
          </el-descriptions>

          <template v-if="detail.examinationData">
            <el-divider>检查数据</el-divider>
            <p style="color: #606266; line-height: 1.8">{{ detail.examinationData }}</p>
          </template>

          <el-divider>AI 分析结果</el-divider>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="病情摘要">{{ detail.aiSummary || '-' }}</el-descriptions-item>
            <el-descriptions-item label="诊断结论">{{ detail.aiDiagnosis || '-' }}</el-descriptions-item>
            <el-descriptions-item label="治疗方案">{{ detail.aiTreatment || '-' }}</el-descriptions-item>
            <el-descriptions-item label="康复建议">{{ detail.aiRecommendation || '-' }}</el-descriptions-item>
          </el-descriptions>

          <el-alert type="warning" :closable="false" style="margin-top: 16px" description="本报告由AI辅助生成，仅供医疗参考，不作为正式诊断依据。" />
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { generateReport, getReportList, getReportDetail, exportReportPdf, confirmReport } from '@/api/report'
import type { MedicalReport, ReportGenerateDto, ReportListParams } from '@/types'

const loading = ref(false)
const reports = ref<MedicalReport[]>([])
const total = ref(0)
const showGenerate = ref(false)
const genLoading = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<MedicalReport | null>(null)
const genFormRef = ref<FormInstance>()

const reportStatusMap: Record<number, string> = { 0: '草稿', 1: '已确认', 2: '已作废' }
const reportStatusType: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }

const queryParams = reactive<ReportListParams>({ page: 1, size: 10 })

const genForm = reactive<ReportGenerateDto>({
  medicalRecordId: undefined,
  reportType: 'EXAMINATION',
  title: '',
  examinationData: '',
})

const genRules: FormRules<ReportGenerateDto> = {
  reportType: [{ required: true, message: '请选择报告类型', trigger: 'change' }],
  title: [{ required: true, message: '请输入报告标题', trigger: 'blur' }],
  examinationData: [{ required: true, message: '请输入检查数据', trigger: 'blur' }],
}

async function loadReports() {
  loading.value = true
  try {
    const res = await getReportList(queryParams)
    reports.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function handleGenerate() {
  await genFormRef.value?.validate()
  genLoading.value = true
  try {
    await generateReport(genForm)
    ElMessage.success('报告生成成功')
    showGenerate.value = false
    loadReports()
  } finally {
    genLoading.value = false
  }
}

async function showDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getReportDetail(id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

async function handleExportPdf(id: number) {
  try {
    const res = await exportReportPdf(id)
    if (res.data) {
      ElMessage.success('PDF导出成功，路径：' + res.data)
    }
  } catch { /* error handled by interceptor */ }
}

async function handleConfirm(id: number) {
  try {
    await ElMessageBox.confirm('确认后将无法修改，是否确认此报告？', '确认报告', { type: 'warning' })
    await confirmReport(id)
    ElMessage.success('报告已确认')
    loadReports()
  } catch { /* user cancelled */ }
}

onMounted(loadReports)
</script>

<style scoped>
.header-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; }
</style>
