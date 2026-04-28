<template>
  <div class="page report-container">
    <div class="page-header hero-header">
      <div class="hero-left">
        <div class="hero-title-row">
          <el-icon class="hero-icon"><Document /></el-icon>
          <span class="hero-title">医疗报告</span>
        </div>
        <div class="hero-subtitle">查看报告、导出 PDF，并可通过 AI 从病历生成草稿报告</div>
      </div>
    </div>
    <el-row class="fill-row" :gutter="20">
      <!-- 报告列表 -->
      <el-col :xs="24" :lg="16" class="fill-col">
        <el-card class="fill-card list-card" shadow="hover">
          <template #header>
            <div class="header-row">
              <div class="head-left">
                <span>报告列表</span>
                <el-tag effect="light" type="info" size="small">共 {{ total }} 条</el-tag>
              </div>
              <el-button type="primary" @click="showGenerate = true">AI 生成报告</el-button>
            </div>
          </template>

          <el-table :data="reports" v-loading="loading" stripe>
            <el-table-column prop="title" label="报告标题" min-width="180" />
            <el-table-column prop="reportType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag type="success" size="small">治疗报告</el-tag>
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
                <el-button link type="success" @click="handleExportPdf(row.id, row.title)">导出PDF</el-button>
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
      <el-col :xs="24" :lg="8" class="fill-col">
        <el-card class="fill-card side-card" shadow="hover">
          <template #header>
            <span>报告生成说明</span>
          </template>
          <el-timeline>
            <el-timeline-item type="primary">步骤1：AI分析病历信息</el-timeline-item>
            <el-timeline-item type="primary">步骤2：结合诊断结果分析病情</el-timeline-item>
            <el-timeline-item type="primary">步骤3：参考历史记录判断慢性病</el-timeline-item>
            <el-timeline-item type="primary">步骤4：综合分析给出诊断</el-timeline-item>
            <el-timeline-item type="primary">步骤5：提出治疗方案和建议</el-timeline-item>
          </el-timeline>
          <el-alert type="warning" :closable="false" description="AI生成的报告为草稿状态，需确认后生效。报告内容仅供参考，不作为正式医疗依据。" />
        </el-card>
      </el-col>
    </el-row>

    <!-- AI生成报告对话框 -->
    <el-dialog v-model="showGenerate" title="AI 生成医疗报告" width="1400px" @open="loadMedicalRecords" @close="cancelGenerate">
      <div class="generate-report-container">
        <!-- 左侧：病历列表 -->
        <div class="medical-records-section">
          <div class="section-header">
            <h3>选择病历</h3>
            <el-button link type="primary" @click="loadMedicalRecords">刷新</el-button>
          </div>
          <div v-loading="recordsLoading" class="records-list">
            <div
              v-for="record in medicalRecords"
              :key="record.id"
              class="record-item"
              :class="{ selected: genForm.medicalRecordId === record.id }"
              @click="selectMedicalRecord(record)"
            >
              <div class="record-header">
                <div class="record-date">{{ record.visitDate }}</div>
                <el-tag size="small" type="success">已完成</el-tag>
              </div>
              <div class="record-content">
                <div class="record-doctor">
                  <el-icon><User /></el-icon>
                  {{ record.doctorName }}
                  <span class="record-dept">{{ record.departmentName }}</span>
                </div>
                <div class="record-diagnosis">
                  <strong>诊断：</strong>{{ record.diagnosis }}
                </div>
                <div class="record-complaint">
                  <strong>主诉：</strong>{{ record.chiefComplaint }}
                </div>
              </div>
            </div>
            <div v-if="medicalRecords.length === 0" class="empty-records">
              <el-empty description="暂无病历记录" />
            </div>
          </div>
        </div>

        <!-- 中间：报告生成表单 -->
        <div class="report-form-section">
          <h3>报告信息</h3>
          <el-form ref="genFormRef" :model="genForm" :rules="genRules" label-width="100px">
            <el-form-item label="报告类型">
              <el-tag type="success" size="large">治疗报告</el-tag>
            </el-form-item>
            <el-form-item label="报告标题" prop="title">
              <el-input v-model="genForm.title" placeholder="如：高血压治疗报告" />
            </el-form-item>

            <!-- 已选病历预览 -->
            <el-form-item v-if="selectedRecord" label="已选病历">
              <div class="selected-record-preview">
                <div class="preview-row">
                  <span class="label">就诊日期：</span>
                  <span class="value">{{ selectedRecord.visitDate }}</span>
                </div>
                <div class="preview-row">
                  <span class="label">医生：</span>
                  <span class="value">{{ selectedRecord.doctorName }}（{{ selectedRecord.departmentName }}）</span>
                </div>
                <div class="preview-row">
                  <span class="label">诊断：</span>
                  <span class="value">{{ selectedRecord.diagnosis }}</span>
                </div>
                <div class="preview-row">
                  <span class="label">治疗方案：</span>
                  <span class="value">{{ selectedRecord.treatmentPlan }}</span>
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 右侧：实时思维链展示 -->
        <div class="thought-chain-section">
          <h3>
            <span>AI 思维链</span>
            <el-progress v-if="isGenerating" :percentage="genProgress" :stroke-width="8" />
          </h3>
          <div v-if="isGenerating || generatedThoughtChain" class="thought-chain-display">
            <div v-for="step in thoughtSteps" :key="step.step" class="thought-step-item" :class="{ active: step.active, completed: step.completed }">
              <div class="step-indicator">
                <div class="step-circle">
                  <span v-if="step.completed">✓</span>
                  <span v-else>{{ step.step }}</span>
                </div>
              </div>
              <div class="step-content">
                <div class="step-title">{{ step.title }}</div>
                <div class="step-message">{{ step.message }}</div>
              </div>
            </div>
            <div v-if="generatedThoughtChain" class="thought-chain-detail">
              <div class="detail-header">
                <el-icon><Document /></el-icon>
                详细分析过程
              </div>
              <div class="detail-content">{{ generatedThoughtChain }}</div>
            </div>
          </div>
          <div v-else class="thought-chain-placeholder">
            <el-empty description="点击生成报告后，将实时展示AI的思考过程" />
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="showGenerate = false" :disabled="isGenerating">取消</el-button>
        <el-button type="primary" :loading="isGenerating" :disabled="!genForm.medicalRecordId || isGenerating" @click="handleGenerateStream">
          生成报告
        </el-button>
      </template>
    </el-dialog>

    <!-- 报告详情对话框 -->
    <el-dialog v-model="detailVisible" title="报告详情" width="800px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="报告标题" :span="2">{{ detail.title }}</el-descriptions-item>
            <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
            <el-descriptions-item label="医生">{{ detail.doctorName }}</el-descriptions-item>
            <el-descriptions-item label="报告类型">治疗报告</el-descriptions-item>
            <el-descriptions-item label="状态">{{ reportStatusMap[detail.status] }}</el-descriptions-item>
          </el-descriptions>

          <el-divider>AI 思维链分析</el-divider>
          <div class="thought-chain-container">
            <div class="thought-steps">
              <div class="thought-step">
                <div class="step-header">
                  <div class="step-number">步骤 1</div>
                  <div class="step-title">分析主诉和现病史</div>
                </div>
                <div class="step-content">提取关键症状信息，分析症状的持续时间、严重程度和发展趋势</div>
              </div>
              <div class="thought-step">
                <div class="step-header">
                  <div class="step-number">步骤 2</div>
                  <div class="step-title">结合检查结果分析</div>
                </div>
                <div class="step-content">根据主诉和现病史，结合各项检查结果，分析可能的疾病类型和范围</div>
              </div>
              <div class="thought-step">
                <div class="step-header">
                  <div class="step-number">步骤 3</div>
                  <div class="step-title">参考历史记录</div>
                </div>
                <div class="step-content">查看患者的历史就诊记录，判断是否存在慢性病、复发倾向或相关疾病史</div>
              </div>
              <div class="thought-step">
                <div class="step-header">
                  <div class="step-number">步骤 4</div>
                  <div class="step-title">综合分析诊断</div>
                </div>
                <div class="step-content">综合以上所有信息，得出最可能的诊断结论，包括疾病名称、分型和严重程度</div>
              </div>
              <div class="thought-step">
                <div class="step-header">
                  <div class="step-number">步骤 5</div>
                  <div class="step-title">治疗方案和建议</div>
                </div>
                <div class="step-content">基于诊断结果，提出具体的治疗方案（用药、治疗措施）和康复建议（生活、饮食、复查）</div>
              </div>
            </div>
            <div v-if="detail.aiThoughtChain" class="thought-detail">
              <div class="thought-detail-header">AI 详细分析</div>
              <div class="thought-detail-content">{{ detail.aiThoughtChain }}</div>
            </div>
          </div>

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
import { generateReport, generateReportStream, getReportList, getReportDetail, exportReportPdf, confirmReport } from '@/api/report'
import { getMedicalRecords } from '@/api/patient'
import type { MedicalReport, ReportGenerateDto, MedicalRecord } from '@/types'
import { User, Document } from '@element-plus/icons-vue'

const loading = ref(false)
const reports = ref<MedicalReport[]>([])
const total = ref(0)
const showGenerate = ref(false)
const genLoading = ref(false)
const isGenerating = ref(false)
const recordsLoading = ref(false)
const medicalRecords = ref<MedicalRecord[]>([])
const selectedRecord = ref<MedicalRecord | null>(null)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<MedicalReport | null>(null)
const genFormRef = ref<FormInstance>()

// 流式生成相关
const genProgress = ref(0)
const thoughtSteps = ref<Array<{ step: number; title: string; message: string; active: boolean; completed: boolean }>>([
  { step: 1, title: '开始分析', message: '', active: false, completed: false },
  { step: 2, title: '分析主诉和现病史', message: '', active: false, completed: false },
  { step: 3, title: '结合检查结果分析', message: '', active: false, completed: false },
  { step: 4, title: '参考历史记录', message: '', active: false, completed: false },
  { step: 5, title: '综合分析诊断', message: '', active: false, completed: false },
  { step: 6, title: '提出治疗方案和建议', message: '', active: false, completed: false },
])
const generatedThoughtChain = ref('')

let cancelGeneration: (() => void) | null = null

const reportStatusMap: Record<number, string> = { 0: '草稿', 1: '已确认', 2: '已作废' }
const reportStatusType: Record<number, string> = { 0: 'warning', 1: 'success', 2: 'info' }

const queryParams = reactive({ page: 1, size: 10 })

const genForm = reactive<ReportGenerateDto>({
  medicalRecordId: undefined,
  reportType: 'TREATMENT',
  title: '',
  examinationData: '',
})

const genRules: FormRules<ReportGenerateDto> = {
  title: [{ required: true, message: '请输入报告标题', trigger: 'blur' }],
}

async function loadReports() {
  loading.value = true
  try {
    const res = await getReportList(queryParams)
    reports.value = res.data || []
    total.value = res.total || 0
  } catch {
    reports.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadMedicalRecords() {
  recordsLoading.value = true
  try {
    const res = await getMedicalRecords({ page: 1, size: 100 })
    medicalRecords.value = res.data || []
  } finally {
    recordsLoading.value = false
  }
}

function selectMedicalRecord(record: MedicalRecord) {
  genForm.medicalRecordId = record.id
  selectedRecord.value = record
}

function handleGenerateStream() {
  if (!genForm.medicalRecordId) {
    ElMessage.warning('请先选择病历')
    return
  }
  genFormRef.value?.validate((valid) => {
    if (valid) {
      startStreamGeneration()
    }
  })
}

function startStreamGeneration() {
  // 重置状态
  isGenerating.value = true
  genProgress.value = 0
  generatedThoughtChain.value = ''
  thoughtSteps.value.forEach(step => {
    step.active = false
    step.completed = false
    step.message = ''
  })

  // 调用流式生成
  cancelGeneration = generateReportStream(
    genForm,
    // onStep
    (step, title, message, progress) => {
      genProgress.value = progress
      const stepIndex = step - 1
      if (stepIndex >= 0 && stepIndex < thoughtSteps.value.length) {
        thoughtSteps.value.forEach((s, i) => {
          s.active = i === stepIndex
          s.completed = i < stepIndex
        })
        thoughtSteps.value[stepIndex].title = title
        // 处理转义的换行符
        thoughtSteps.value[stepIndex].message = message.replace(/\\n/g, '\n').replace(/'/g, "'")
      }
    },
    // onThoughtChain
    (thoughtChain) => {
      generatedThoughtChain.value = thoughtChain
    },
    // onComplete
    (report) => {
      ElMessage.success('报告生成成功')
      isGenerating.value = false
      genProgress.value = 100
      thoughtSteps.value.forEach(s => s.completed = true)
      showGenerate.value = false
      loadReports()
    },
    // onError
    (error) => {
      ElMessage.error(error || '生成失败')
      isGenerating.value = false
      cancelGeneration = null
    }
  )
}

function cancelGenerate() {
  if (cancelGeneration) {
    cancelGeneration()
    cancelGeneration = null
  }
  isGenerating.value = false
  generatedThoughtChain.value = ''
  thoughtSteps.value.forEach(step => {
    step.active = false
    step.completed = false
    step.message = ''
  })
  genProgress.value = 0
}

async function handleGenerate() {
  if (!genForm.medicalRecordId) {
    ElMessage.warning('请先选择病历')
    return
  }
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

async function handleExportPdf(id: number, title: string) {
  try {
    await exportReportPdf(id, title)
    ElMessage.success('PDF导出成功')
  } catch (error) {
    console.error('导出失败:', error)
  }
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
.report-container {
  position: relative;
  min-height: calc(100vh - var(--his-header-height));
  background:
    radial-gradient(920px 420px at -8% -10%, rgba(var(--his-primary-rgb), 0.18), transparent 64%),
    radial-gradient(820px 360px at 108% 4%, rgba(var(--his-primary-rgb), 0.12), transparent 68%),
    linear-gradient(160deg, #f4fdf8 0%, #eefaf3 52%, #f8fffb 100%);
}

.report-container .page-header,
.report-container .el-row {
  position: relative;
  z-index: 1;
}

.report-container :deep(.el-card) {
  border: 1px solid rgba(var(--his-primary-rgb), 0.2);
  background: rgba(255, 255, 255, 0.86);
  backdrop-filter: blur(14px) saturate(130%);
  box-shadow:
    0 16px 36px rgba(18, 56, 38, 0.1),
    0 8px 20px rgba(var(--his-primary-rgb), 0.08);
}

.hero-header {
  margin-bottom: 16px;
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

.report-container :deep(.el-card__header) {
  border-bottom: 1px solid rgba(var(--his-primary-rgb), 0.14);
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.12), rgba(var(--his-primary-rgb), 0.03));
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

.list-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.list-card :deep(.el-table) {
  flex: 1;
  border-radius: 14px;
  overflow: hidden;
}

.list-card :deep(.el-table th.el-table__cell) {
  background: rgba(var(--his-primary-rgb), 0.1);
}

.list-card :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(var(--his-primary-rgb), 0.08) !important;
}

.list-card :deep(.el-pagination) {
  margin-top: auto;
}

.side-card :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.side-card :deep(.el-alert) {
  margin-top: auto;
  border-color: rgba(var(--his-primary-rgb), 0.22);
}

/* 报告生成对话框 */
.generate-report-container {
  display: flex;
  gap: 20px;
  height: 650px;
}

.medical-records-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(var(--his-primary-rgb), 0.18);
  padding-right: 20px;
  min-width: 280px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--his-text);
}

.records-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
}

.record-item {
  border: 1px solid rgba(var(--his-primary-rgb), 0.16);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.2s ease, background 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
  background: rgba(255, 255, 255, 0.82);
}

.record-item:hover {
  border-color: rgba(var(--his-primary-rgb), 0.35);
  box-shadow: 0 12px 26px rgba(var(--his-primary-rgb), 0.14);
  transform: translateY(-1px);
}

.record-item.selected {
  border-color: rgba(var(--his-primary-rgb), 0.45);
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.16), rgba(var(--his-primary-rgb), 0.06));
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-date {
  font-size: 16px;
  font-weight: 700;
  color: var(--his-text);
}

.record-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-doctor,
.record-diagnosis,
.record-complaint {
  font-size: 13px;
  line-height: 1.5;
  color: var(--his-text-2);
}

.record-dept {
  margin-left: auto;
  font-size: 12px;
  color: var(--his-text-2);
}

.record-diagnosis strong,
.record-complaint strong {
  color: var(--his-text);
}

.empty-records {
  padding: 60px 20px;
  text-align: center;
}

.report-form-section {
  flex: 1;
  overflow-y: auto;
  min-width: 280px;
  border-right: 1px solid rgba(var(--his-primary-rgb), 0.18);
  padding-right: 20px;
}

.report-form-section h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--his-text);
}

.selected-record-preview {
  background: rgba(var(--his-primary-rgb), 0.06);
  border-radius: 14px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.18);
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.preview-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.preview-row .label {
  color: var(--his-text-2);
  font-weight: 500;
}

.preview-row .value {
  color: var(--his-text);
  font-weight: 600;
}

.thought-chain-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.thought-chain-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
  color: var(--his-text);
}

.thought-chain-section .el-progress {
  flex: 1;
}

.thought-chain-display {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
  padding-right: 4px;
}

.thought-chain-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thought-step-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  background: rgba(var(--his-primary-rgb), 0.04);
  border-radius: 14px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.14);
  transition: all 0.2s ease;
}

.thought-step-item.active {
  background: rgba(var(--his-primary-rgb), 0.12);
  border-color: rgba(var(--his-primary-rgb), 0.34);
  box-shadow: 0 6px 16px rgba(var(--his-primary-rgb), 0.12);
}

.thought-step-item.completed {
  background: rgba(var(--his-primary-rgb), 0.06);
  border-color: rgba(var(--his-primary-rgb), 0.2);
  opacity: 0.84;
}

.step-indicator {
  flex-shrink: 0;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(var(--his-primary-rgb), 0.18);
  color: var(--his-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  transition: all 0.2s ease;
}

.thought-step-item.active .step-circle {
  background: var(--his-primary);
  color: white;
  transform: scale(1.04);
}

.thought-step-item.completed .step-circle {
  background: #67c23a;
  color: white;
}

.step-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--his-text);
}

.step-message {
  font-size: 13px;
  color: var(--his-text-2);
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 120px;
  overflow-y: auto;
}

.thought-chain-detail {
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(var(--his-primary-rgb), 0.24);
  border-radius: 10px;
  padding: 16px;
  flex-shrink: 0;
}

.detail-header {
  font-size: 14px;
  font-weight: 600;
  color: var(--his-primary);
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-content {
  font-size: 13px;
  color: var(--his-text);
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 200px;
  overflow-y: auto;
}

.thought-chain-container {
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.08), rgba(var(--his-primary-rgb), 0.02));
  border-radius: 12px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.16);
  padding: 20px;
  margin-bottom: 20px;
}

.thought-steps {
  display: grid;
  gap: 16px;
  margin-bottom: 20px;
}

.thought-step {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(var(--his-primary-rgb), 0.16);
  border-radius: 10px;
  padding: 16px;
  transition: all 0.2s ease;
}

.thought-step:hover {
  box-shadow: 0 8px 16px rgba(var(--his-primary-rgb), 0.12);
  transform: translateY(-1px);
}

.step-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.step-number {
  width: 80px;
  height: 32px;
  background: linear-gradient(135deg, var(--his-primary) 0%, var(--his-accent) 100%);
  color: white;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
}

.thought-detail {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(var(--his-primary-rgb), 0.24);
  border-radius: 10px;
  padding: 16px;
}

.thought-detail-header {
  font-size: 15px;
  font-weight: 600;
  color: var(--his-primary);
  margin-bottom: 12px;
}

.thought-detail-content {
  font-size: 14px;
  color: var(--his-text);
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
}

.report-container :deep(.el-dialog) {
  border-radius: 18px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.2);
  overflow: hidden;
}

.report-container :deep(.el-dialog__header) {
  margin-bottom: 0;
  background: linear-gradient(180deg, rgba(var(--his-primary-rgb), 0.14), rgba(255, 255, 255, 0.03));
}

@media (max-width: 1200px) {
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

  .generate-report-container {
    flex-direction: column;
    height: auto;
    max-height: 70vh;
    overflow-y: auto;
  }

  .medical-records-section,
  .report-form-section {
    border-right: none;
    padding-right: 0;
  }
}
</style>
