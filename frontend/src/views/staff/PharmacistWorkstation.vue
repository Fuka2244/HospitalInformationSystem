<template>
  <div class="staff-page">
    <section class="staff-header">
      <div>
        <div class="eyebrow">Pharmacy Workstation</div>
        <h1>药师工作台</h1>
        <p>处理待审处方、完成发药、维护库存并追踪低库存风险。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </section>

    <el-row :gutter="16" class="metric-row">
      <el-col :xs="12" :md="6"><el-card shadow="never" class="metric-card"><span>待审处方</span><strong>{{ prescriptions.length }}</strong></el-card></el-col>
      <el-col :xs="12" :md="6"><el-card shadow="never" class="metric-card"><span>库存批次</span><strong>{{ inventory.length }}</strong></el-card></el-col>
      <el-col :xs="12" :md="6"><el-card shadow="never" class="metric-card danger"><span>低库存</span><strong>{{ lowStock.length }}</strong></el-card></el-col>
      <el-col :xs="12" :md="6"><el-card shadow="never" class="metric-card"><span>流水记录</span><strong>{{ stockLogs.length }}</strong></el-card></el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="portal-tabs">
      <el-tab-pane label="处方审核" name="prescriptions">
        <el-card shadow="never" class="work-card">
          <el-table :data="prescriptions" v-loading="loading" stripe>
            <el-table-column prop="id" label="处方号" width="100" />
            <el-table-column prop="patientId" label="患者 ID" min-width="150" />
            <el-table-column prop="doctorId" label="医生 ID" width="110" />
            <el-table-column prop="prescriptionDate" label="开方时间" min-width="170" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }"><el-tag>{{ prescriptionStatus(row.status) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="操作" width="220" fixed="right">
              <template #default="{ row }">
                <el-button link type="success" @click="openAudit(row, 1)">通过</el-button>
                <el-button link type="danger" @click="openAudit(row, 2)">驳回</el-button>
                <el-button link type="primary" :disabled="row.status !== 1" @click="handleDispense(row.id)">发药</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="库存管理" name="inventory">
        <el-card shadow="never" class="work-card">
          <template #header>
            <div class="card-head">
              <strong>药品库存</strong>
              <el-button type="primary" @click="openInventory()">新增入库</el-button>
            </div>
          </template>
          <el-table :data="inventory" v-loading="loading" stripe>
            <el-table-column prop="medicineId" label="药品 ID" width="100" />
            <el-table-column prop="quantity" label="库存" width="100" />
            <el-table-column prop="minStock" label="下限" width="90" />
            <el-table-column prop="maxStock" label="上限" width="90" />
            <el-table-column prop="supplier" label="供应商" min-width="130" />
            <el-table-column prop="batchNumber" label="批号" min-width="130" />
            <el-table-column prop="expiryDate" label="有效期" min-width="160" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.quantity <= row.minStock ? 'danger' : 'success'">
                  {{ row.quantity <= row.minStock ? '低库存' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }"><el-button link type="primary" @click="openInventory(row)">调整</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="库存流水" name="logs">
        <el-card shadow="never" class="work-card">
          <el-table :data="stockLogs" v-loading="loading" stripe>
            <el-table-column prop="createTime" label="时间" min-width="170" />
            <el-table-column prop="medicineId" label="药品 ID" width="100" />
            <el-table-column prop="operationType" label="操作" width="120" />
            <el-table-column prop="beforeStock" label="变更前" width="100" />
            <el-table-column prop="quantity" label="变化量" width="100" />
            <el-table-column prop="afterStock" label="变更后" width="100" />
            <el-table-column prop="operator" label="操作人" width="120" />
            <el-table-column prop="remark" label="备注" min-width="180" />
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="auditVisible" title="处方审核" width="460px">
      <el-alert :title="auditForm.auditStatus === 1 ? '确认通过该处方' : '确认驳回该处方'" type="info" :closable="false" />
      <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" class="dialog-field" placeholder="审核备注" />
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="handleAudit">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inventoryVisible" :title="inventoryForm.id ? '调整库存' : '新增入库'" width="680px">
      <el-form :model="inventoryForm" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="药品 ID"><el-input-number v-model="inventoryForm.medicineId" :min="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="数量"><el-input-number v-model="inventoryForm.quantity" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库存下限"><el-input-number v-model="inventoryForm.minStock" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="库存上限"><el-input-number v-model="inventoryForm.maxStock" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="采购价"><el-input-number v-model="inventoryForm.purchasePrice" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="销售价"><el-input-number v-model="inventoryForm.sellingPrice" :min="0" :precision="2" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="供应商"><el-input v-model="inventoryForm.supplier" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="批号"><el-input v-model="inventoryForm.batchNumber" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="有效期"><el-date-picker v-model="inventoryForm.expiryDate" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" style="width:100%" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="inventoryVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="saveInventory">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { addMedicineInventory, auditPrescription, dispenseMedicine, getLowStockMedicines, getMedicineInventory, getPendingPrescriptions, getStockLogs, updateMedicineInventory } from '@/api/pharmacist'
import type { MedicineInventory, PrescriptionRecord } from '@/types'

const activeTab = ref('prescriptions')
const loading = ref(false)
const actionLoading = ref(false)
const prescriptions = ref<PrescriptionRecord[]>([])
const inventory = ref<MedicineInventory[]>([])
const lowStock = ref<MedicineInventory[]>([])
const stockLogs = ref<any[]>([])
const auditVisible = ref(false)
const inventoryVisible = ref(false)
const auditForm = reactive({ prescriptionId: 0, auditStatus: 1, auditRemark: '' })
const inventoryForm = reactive<any>({ id: undefined, medicineId: 1, quantity: 0, minStock: 10, maxStock: 1000, purchasePrice: 0, sellingPrice: 0, supplier: '', batchNumber: '', expiryDate: '' })

function prescriptionStatus(status: number) {
  return ({ 0: '待审核', 1: '已通过', 2: '已驳回' } as Record<number, string>)[status] || `状态 ${status}`
}

async function loadData() {
  loading.value = true
  try {
    const [prescriptionRes, inventoryRes, lowStockRes, logRes] = await Promise.all([
      getPendingPrescriptions(),
      getMedicineInventory(),
      getLowStockMedicines(),
      getStockLogs(),
    ])
    prescriptions.value = prescriptionRes.data || []
    inventory.value = inventoryRes.data || []
    lowStock.value = lowStockRes.data || []
    stockLogs.value = logRes.data || []
  } finally {
    loading.value = false
  }
}

function openAudit(row: PrescriptionRecord, status: number) {
  Object.assign(auditForm, { prescriptionId: row.id, auditStatus: status, auditRemark: '' })
  auditVisible.value = true
}

async function handleAudit() {
  actionLoading.value = true
  try {
    await auditPrescription(auditForm)
    ElMessage.success('审核完成')
    auditVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}

async function handleDispense(id: number) {
  actionLoading.value = true
  try {
    await dispenseMedicine(id)
    ElMessage.success('发药完成')
    loadData()
  } finally {
    actionLoading.value = false
  }
}

function openInventory(row?: MedicineInventory) {
  Object.assign(inventoryForm, row || { id: undefined, medicineId: 1, quantity: 0, minStock: 10, maxStock: 1000, purchasePrice: 0, sellingPrice: 0, supplier: '', batchNumber: '', expiryDate: '' })
  inventoryVisible.value = true
}

async function saveInventory() {
  actionLoading.value = true
  try {
    const { id, ...payload } = inventoryForm
    if (id) await updateMedicineInventory(id, payload)
    else await addMedicineInventory(payload)
    ElMessage.success('库存已保存')
    inventoryVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.staff-page { min-height: calc(100vh - var(--his-header-height)); padding: 18px; background: linear-gradient(160deg, #f7fbff 0%, #effaf5 54%, #ffffff 100%); }
.staff-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 16px; padding: 22px 26px; border: 1px solid rgba(var(--his-primary-rgb), .18); border-radius: 8px; background: rgba(255,255,255,.88); }
.eyebrow { color: var(--his-primary); font-size: 12px; font-weight: 800; text-transform: uppercase; }
h1 { margin: 4px 0 6px; font-size: 30px; line-height: 1.1; color: var(--his-text); }
p { margin: 0; color: var(--his-text-2); }
.metric-row { margin-bottom: 14px; }
.metric-card { border-radius: 8px; }
.metric-card :deep(.el-card__body) { display:flex; flex-direction:column; gap:8px; }
.metric-card span { color: var(--his-text-2); font-size:13px; }
.metric-card strong { font-size:28px; color: var(--his-text); }
.metric-card.danger strong { color: #d93025; }
.work-card { border-radius: 8px; }
.card-head { display:flex; align-items:center; justify-content:space-between; gap:12px; }
.portal-tabs :deep(.el-tabs__content) { overflow: visible; }
.dialog-field { margin-top: 14px; }
@media (max-width: 768px) { .staff-header { align-items: flex-start; flex-direction: column; } }
</style>
