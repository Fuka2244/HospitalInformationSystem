<template>
  <div class="medicine-container" style="padding: 20px">
    <el-row :gutter="20">
      <!-- 药品列表 -->
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="header-row">
              <span>药品信息查询</span>
              <div>
                <el-input v-model="queryParams.keyword" placeholder="搜索药品名称/功效" clearable style="width: 220px; margin-right: 8px" @keyup.enter="handleSearch" />
                <el-select v-model="queryParams.category" placeholder="药品分类" clearable style="width: 140px; margin-right: 8px">
                  <el-option label="化学药" value="化学药" />
                  <el-option label="中成药" value="中成药" />
                  <el-option label="生物制品" value="生物制品" />
                </el-select>
                <el-button type="primary" @click="handleSearch">搜索</el-button>
              </div>
            </div>
          </template>

          <el-table :data="medicines" v-loading="loading" stripe>
            <el-table-column prop="name" label="药品名称" width="160" />
            <el-table-column prop="genericName" label="通用名" width="120" />
            <el-table-column prop="category" label="分类" width="90">
              <template #default="{ row }">
                <el-tag :type="row.category === '化学药' ? '' : row.category === '中成药' ? 'success' : 'warning'" size="small">
                  {{ row.category }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="specification" label="规格" width="120" />
            <el-table-column prop="efficacy" label="功效" min-width="200" show-overflow-tooltip />
            <el-table-column prop="price" label="价格" width="90">
              <template #default="{ row }">¥{{ row.price?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="showDetail(row.id)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="queryParams.page"
            v-model:page-size="queryParams.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            style="margin-top: 16px; justify-content: flex-end"
            @change="loadMedicines"
          />
        </el-card>
      </el-col>

      <!-- AI药品推荐 -->
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <span>AI 药品推荐</span>
          </template>
          <el-input v-model="aiSymptom" type="textarea" :rows="3" placeholder="请描述您的症状，如：头痛发热" />
          <el-button type="primary" :loading="aiLoading" style="width: 100%; margin-top: 12px" @click="handleAiRecommend">
            AI 推荐药品
          </el-button>
          <div v-if="aiResults.length > 0" class="ai-results">
            <el-divider>推荐结果</el-divider>
            <el-alert type="warning" :closable="false" style="margin-bottom: 12px">
              AI推荐仅供参考，请遵医嘱用药
            </el-alert>
            <el-card v-for="(item, idx) in aiResults" :key="idx" shadow="never" style="margin-bottom: 12px">
              <template #header>
                <strong>{{ item.medicineName }}</strong>
              </template>
              <p><strong>推荐理由：</strong>{{ item.reason }}</p>
              <p><strong>用法用量：</strong>{{ item.dosage }}</p>
              <p><strong>注意事项：</strong>{{ item.precautions }}</p>
            </el-card>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 药品详情对话框 -->
    <el-dialog v-model="detailVisible" title="药品详情" width="600px">
      <div v-loading="detailLoading">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="药品名称">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="通用名">{{ detail.genericName }}</el-descriptions-item>
            <el-descriptions-item label="分类">{{ detail.category }}</el-descriptions-item>
            <el-descriptions-item label="规格">{{ detail.specification }}</el-descriptions-item>
            <el-descriptions-item label="生产厂家" :span="2">{{ detail.manufacturer }}</el-descriptions-item>
            <el-descriptions-item label="价格">¥{{ detail.price?.toFixed(2) }}</el-descriptions-item>
            <el-descriptions-item label="库存">{{ detail.stock }}</el-descriptions-item>
            <el-descriptions-item label="成分" :span="2">{{ detail.ingredients }}</el-descriptions-item>
            <el-descriptions-item label="功效" :span="2">{{ detail.efficacy }}</el-descriptions-item>
            <el-descriptions-item label="副作用" :span="2">{{ detail.sideEffects }}</el-descriptions-item>
            <el-descriptions-item label="禁忌" :span="2">{{ detail.contraindications }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMedicineList, getMedicineDetail, aiRecommendMedicine } from '@/api/medicine'
import type { Medicine, MedicineListParams, MedicineRecommendation } from '@/types'

const loading = ref(false)
const medicines = ref<Medicine[]>([])
const total = ref(0)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<Medicine | null>(null)
const aiSymptom = ref('')
const aiLoading = ref(false)
const aiResults = ref<MedicineRecommendation[]>([])

const queryParams = reactive<MedicineListParams>({ page: 1, size: 10 })

async function loadMedicines() {
  loading.value = true
  try {
    const res = await getMedicineList(queryParams)
    medicines.value = res.data || []
    total.value = res.total || 0
  } finally {
    loading.value = false
  }
}

async function showDetail(id: number) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getMedicineDetail(id)
    detail.value = res.data
  } finally {
    detailLoading.value = false
  }
}

async function handleAiRecommend() {
  if (!aiSymptom.value.trim()) {
    ElMessage.warning('请描述您的症状')
    return
  }
  aiLoading.value = true
  try {
    const res = await aiRecommendMedicine({ symptom: aiSymptom.value })
    aiResults.value = res.data || []
    if (aiResults.value.length === 0) {
      ElMessage.info('暂未找到合适的推荐药品')
    }
  } finally {
    aiLoading.value = false
  }
}

function handleSearch() {
  queryParams.page = 1
  loadMedicines()
}

onMounted(loadMedicines)
</script>

<style scoped>
.header-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; }
.ai-results p { margin: 4px 0; font-size: 13px; color: #606266; }
</style>
