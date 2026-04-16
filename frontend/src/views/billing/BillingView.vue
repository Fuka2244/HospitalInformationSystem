<template>
  <div class="billing-container" style="padding: 20px">
    <el-row :gutter="20">
      <!-- 费用列表 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="header-row">
              <span>医疗费用查询</span>
              <div>
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

      <!-- AI费用解释 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>AI 费用解释</span>
          </template>
          <el-date-picker v-model="aiDateRange" type="daterange" range-separator="至" start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" style="width: 100%; margin-bottom: 12px" />
          <el-input v-model="aiQuestion" type="textarea" :rows="3" placeholder="请输入您的问题，如：为什么这么贵？" />
          <el-button type="primary" :loading="aiLoading" style="width: 100%; margin-top: 12px" @click="handleAiExplain">
            AI 解释费用
          </el-button>
          <div v-if="explanation" class="ai-result">
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
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBillingList, aiExplainBilling } from '@/api/billing'
import type { Billing, BillingQueryParams, BillingExplanation } from '@/types'

const loading = ref(false)
const billings = ref<Billing[]>([])
const total = ref(0)
const aiLoading = ref(false)
const aiQuestion = ref('')
const aiDateRange = ref<[string, string] | null>(null)
const explanation = ref<BillingExplanation | null>(null)
const dateRange = ref<[string, string] | null>(null)

const itemTypeMap: Record<string, string> = { REGISTRATION: '挂号', EXAMINATION: '检查', MEDICINE: '药品', OTHER: '其他' }
const itemTypeTag: Record<string, string> = { REGISTRATION: '', EXAMINATION: 'warning', MEDICINE: 'success', OTHER: 'info' }
const statusMap: Record<number, string> = { 0: '未支付', 1: '已支付', 2: '已退款' }
const statusTagType: Record<number, string> = { 0: 'danger', 1: 'success', 2: 'info' }

const queryParams = reactive<BillingQueryParams>({ page: 1, size: 10 })

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
    const res = await getBillingList(queryParams)
    billings.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function handleAiExplain() {
  if (!aiQuestion.value.trim()) {
    ElMessage.warning('请输入您的问题')
    return
  }
  aiLoading.value = true
  try {
    const params: any = { question: aiQuestion.value }
    if (aiDateRange.value) {
      params.startDate = aiDateRange.value[0]
      params.endDate = aiDateRange.value[1]
    }
    const res = await aiExplainBilling(params)
    explanation.value = res.data
  } finally {
    aiLoading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  loadBillings()
}

onMounted(loadBillings)
</script>

<style scoped>
.header-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; }
.ai-result { margin-top: 8px; }
</style>
