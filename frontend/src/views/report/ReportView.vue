<template>
  <div class="page report-container">
    <div class="page-header">
      <div>
        <div class="page-title">医疗报告</div>
        <div class="page-subtitle">查看报告、导出 PDF，并可通过 AI 从病历生成草稿报告</div>
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
.header-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.head-left{
  display:flex;
  align-items:center;
  gap: 10px;
  font-weight: 800;
  letter-spacing: 0.2px;
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
.side-card :deep(.el-card__body){
  display:flex;
  flex-direction: column;
  gap: 12px;
}
.side-card :deep(.el-alert){
  margin-top: auto;
}

/* 报告生成对话框 */
.generate-report-container {
  display: flex;
  gap: 20px;
  height: 650px;
}

/* 病历列表部分 */
.medical-records-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  border-right: 1px solid rgba(15, 23, 42, 0.08);
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
}

.records-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px;
}

.record-item {
  border: 1px solid rgba(15, 23, 42, 0.10);
  border-radius: 16px;
  padding: 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(10px);
}

.record-item:hover {
  border-color: rgba(47, 128, 237, 0.20);
  box-shadow: 0 14px 36px rgba(15, 23, 42, 0.10);
  transform: translateY(-1px);
}

.record-item.selected {
  border-color: rgba(47, 128, 237, 0.30);
  background:
    radial-gradient(700px 340px at 20% 0%, rgba(47, 128, 237, 0.42), transparent 60%),
    radial-gradient(700px 340px at 100% 20%, rgba(120, 87, 255, 0.36), transparent 60%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.92) 0%, rgba(15, 23, 42, 0.84) 100%);
  color: white;
}

.record-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.record-date {
  font-size: 16px;
  font-weight: bold;
}

.record-item.selected .record-date {
  color: white;
}

.record-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.record-doctor {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #606266;
}

.record-item.selected .record-doctor {
  color: rgba(255, 255, 255, 0.9);
}

.record-dept {
  margin-left: auto;
  font-size: 13px;
  color: #909399;
}

.record-item.selected .record-dept {
  color: rgba(255, 255, 255, 0.85);
}

.record-diagnosis,
.record-complaint {
  font-size: 13px;
  line-height: 1.5;
  color: #606266;
}

.record-item.selected .record-diagnosis,
.record-item.selected .record-complaint {
  color: rgba(255, 255, 255, 0.9);
}

.record-diagnosis strong,
.record-complaint strong {
  color: #303133;
}

.record-item.selected .record-diagnosis strong,
.record-item.selected .record-complaint strong {
  color: white;
}

.empty-records {
  padding: 60px 20px;
  text-align: center;
}

/* 报告表单部分 */
.report-form-section {
  flex: 1;
  overflow-y: auto;
  min-width: 280px;
  border-right: 1px solid rgba(15, 23, 42, 0.08);
  padding-right: 20px;
}

.report-form-section h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
}

.selected-record-preview {
  background: rgba(15, 23, 42, 0.02);
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.08);
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
  color: #909399;
  font-weight: 500;
}

.preview-row .value {
  color: #303133;
  font-weight: 500;
}

/* 实时思维链展示部分 */
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
  background: rgba(15, 23, 42, 0.02);
  border-radius: 14px;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease;
  border: 1px solid rgba(15, 23, 42, 0.08);
}

.thought-step-item.active {
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.thought-step-item.completed {
  background: #f0f9ff;
  border-color: #67c23a;
  opacity: 0.7;
}

.step-indicator {
  flex-shrink: 0;
}

.step-circle {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: #dcdfe6;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  transition: all 0.3s;
}

.thought-step-item.active .step-circle {
  background: #409eff;
  color: white;
  transform: scale(1.1);
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
  color: #303133;
}

.step-message {
  font-size: 13px;
  color: #606266;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 120px;
  overflow-y: auto;
}

.thought-chain-detail {
  background: white;
  border: 2px solid #409eff;
  border-radius: 8px;
  padding: 16px;
  flex-shrink: 0;
}

.detail-header {
  font-size: 14px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-content {
  font-size: 13px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
  max-height: 200px;
  overflow-y: auto;
}

/* 思维链容器 */
.thought-chain-container {
  background: linear-gradient(135deg, #f5f7fa 0%, #e9ecef 100%);
  border-radius: 12px;
  padding: 20px;
  margin-bottom: 20px;
}

.thought-steps {
  display: grid;
  gap: 16px;
  margin-bottom: 20px;
}

.thought-step {
  background: white;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px;
  transition: all 0.3s;
}

.thought-step:hover {
  box-shadow: 0 2px 12px 0 rgba(64, 158, 255, 0.15);
  transform: translateY(-2px);
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
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: bold;
  flex-shrink: 0;
}

.step-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.step-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  padding-left: 92px;
}

.thought-detail {
  background: white;
  border: 2px solid #409eff;
  border-radius: 8px;
  padding: 16px;
}

.thought-detail-header {
  font-size: 15px;
  font-weight: 600;
  color: #409eff;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.thought-detail-header::before {
  content: '💭';
  font-size: 18px;
}

.thought-detail-content {
  font-size: 14px;
  color: #303133;
  line-height: 1.8;
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
