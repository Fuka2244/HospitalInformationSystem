<template>
  <div class="staff-page">
    <section class="staff-header">
      <div>
        <div class="eyebrow">Admin Console</div>
        <h1>管理员控制台</h1>
        <p>查看全局运营指标、人员与科室数据，维护系统配置。</p>
      </div>
      <el-button type="primary" :loading="loading" @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </section>

    <el-row :gutter="16" class="metric-row">
      <el-col v-for="item in globalCards" :key="item.label" :xs="12" :md="4">
        <el-card shadow="never" class="metric-card"><span>{{ item.label }}</span><strong>{{ item.value }}</strong></el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" :model="query">
        <el-form-item label="开始日期"><el-date-picker v-model="query.startDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item label="结束日期"><el-date-picker v-model="query.endDate" type="date" value-format="YYYY-MM-DD" /></el-form-item>
        <el-form-item><el-button type="primary" @click="loadStatistics">查询统计</el-button></el-form-item>
      </el-form>
      <el-row :gutter="12">
        <el-col v-for="item in periodCards" :key="item.label" :xs="12" :md="4">
          <div class="period-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-tabs v-model="activeTab" class="portal-tabs">
      <el-tab-pane label="人员管理" name="users">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="8">
            <el-card shadow="never" class="work-card"><template #header><strong>患者</strong></template><el-table :data="users.patients" height="360"><el-table-column prop="name" label="姓名" /><el-table-column prop="phone" label="电话" /></el-table></el-card>
          </el-col>
          <el-col :xs="24" :lg="8">
            <el-card shadow="never" class="work-card"><template #header><strong>医生</strong></template><el-table :data="users.doctors" height="360"><el-table-column prop="name" label="姓名" /><el-table-column prop="title" label="职称" /></el-table></el-card>
          </el-col>
          <el-col :xs="24" :lg="8">
            <el-card shadow="never" class="work-card"><template #header><strong>药师</strong></template><el-table :data="users.pharmacists" height="360"><el-table-column prop="name" label="姓名" /><el-table-column prop="phone" label="电话" /></el-table></el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="科室与排班" name="schedule">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="12">
            <el-card shadow="never" class="work-card">
              <template #header><strong>科室列表</strong></template>
              <el-table :data="departments" height="420">
                <el-table-column prop="name" label="科室" width="130" />
                <el-table-column prop="location" label="位置" width="150" />
                <el-table-column prop="description" label="说明" show-overflow-tooltip />
              </el-table>
            </el-card>
          </el-col>
          <el-col :xs="24" :lg="12">
            <el-card shadow="never" class="work-card">
              <template #header><strong>医生资源</strong></template>
              <el-table :data="schedule.doctors" height="420">
                <el-table-column prop="name" label="医生" width="110" />
                <el-table-column prop="title" label="职称" width="120" />
                <el-table-column prop="specialty" label="专长" show-overflow-tooltip />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <el-tab-pane label="系统配置" name="configs">
        <el-card shadow="never" class="work-card">
          <el-table :data="configs" v-loading="loading" stripe>
            <el-table-column prop="configKey" label="配置键" min-width="180" />
            <el-table-column prop="configValue" label="配置值" min-width="180" show-overflow-tooltip />
            <el-table-column prop="configType" label="类型" width="120" />
            <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }"><el-button link type="primary" @click="openConfig(row)">编辑</el-button></template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="configVisible" title="编辑系统配置" width="560px">
      <el-form :model="configForm" label-width="80px">
        <el-form-item label="配置键"><el-input v-model="configForm.configKey" /></el-form-item>
        <el-form-item label="配置值"><el-input v-model="configForm.configValue" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="类型"><el-input v-model="configForm.configType" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="configForm.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="configVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionLoading" @click="saveConfig">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getDepartmentManagement, getDoctorScheduleManagement, getGlobalStatistics, getStatistics, getSystemConfigs, getUserManagement, updateSystemConfig } from '@/api/admin'
import type { AdminGlobalStatistics, AdminScheduleManagement, AdminStatistics, AdminUserManagement, Department, SystemConfig } from '@/types'

const activeTab = ref('users')
const loading = ref(false)
const actionLoading = ref(false)
const configVisible = ref(false)
const globalStats = ref<AdminGlobalStatistics>({})
const statistics = ref<AdminStatistics>({})
const configs = ref<SystemConfig[]>([])
const departments = ref<Department[]>([])
const users = reactive<AdminUserManagement>({ patients: [], doctors: [], pharmacists: [] })
const schedule = reactive<AdminScheduleManagement>({ doctors: [], departments: [] })
const query = reactive({ startDate: '', endDate: '' })
const configForm = reactive<SystemConfig>({ id: 0, configKey: '', configValue: '', configType: '', description: '' })

const globalCards = computed(() => [
  { label: '患者', value: globalStats.value.totalPatients || 0 },
  { label: '医生', value: globalStats.value.totalDoctors || 0 },
  { label: '科室', value: globalStats.value.totalDepartments || 0 },
  { label: '药品', value: globalStats.value.totalMedicines || 0 },
  { label: '今日预约', value: globalStats.value.todayAppointments || 0 },
  { label: '未付账单', value: globalStats.value.unpaidBills || 0 },
])

const periodCards = computed(() => [
  { label: '预约总数', value: statistics.value.totalAppointments || 0 },
  { label: '完成预约', value: statistics.value.completedAppointments || 0 },
  { label: '取消预约', value: statistics.value.cancelledAppointments || 0 },
  { label: '收入总额', value: `¥${Number(statistics.value.totalRevenue || 0).toFixed(2)}` },
  { label: '已付金额', value: `¥${Number(statistics.value.paidAmount || 0).toFixed(2)}` },
  { label: '新增患者', value: statistics.value.newPatients || 0 },
])

async function loadStatistics() {
  const res = await getStatistics(query)
  statistics.value = res.data || {}
}

async function loadData() {
  loading.value = true
  try {
    const [globalRes, configRes, userRes, deptRes, scheduleRes] = await Promise.all([
      getGlobalStatistics(),
      getSystemConfigs(),
      getUserManagement(),
      getDepartmentManagement(),
      getDoctorScheduleManagement(),
      loadStatistics(),
    ])
    globalStats.value = globalRes.data || {}
    configs.value = configRes.data || []
    Object.assign(users, userRes.data || { patients: [], doctors: [], pharmacists: [] })
    departments.value = deptRes.data || []
    Object.assign(schedule, scheduleRes.data || { doctors: [], departments: [] })
  } finally {
    loading.value = false
  }
}

function openConfig(row: SystemConfig) {
  Object.assign(configForm, row)
  configVisible.value = true
}

async function saveConfig() {
  actionLoading.value = true
  try {
    await updateSystemConfig(configForm)
    ElMessage.success('配置已更新')
    configVisible.value = false
    loadData()
  } finally {
    actionLoading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.staff-page { min-height: calc(100vh - var(--his-header-height)); padding: 18px; background: linear-gradient(160deg, #f7fff9 0%, #f0faf6 54%, #ffffff 100%); }
.staff-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 18px; margin-bottom: 16px; padding: 22px 26px; border: 1px solid rgba(var(--his-primary-rgb), .18); border-radius: 8px; background: rgba(255,255,255,.88); }
.eyebrow { color: var(--his-primary); font-size: 12px; font-weight: 800; text-transform: uppercase; }
h1 { margin: 4px 0 6px; font-size: 30px; line-height: 1.1; color: var(--his-text); }
p { margin: 0; color: var(--his-text-2); }
.metric-row, .filter-card { margin-bottom: 14px; }
.metric-card, .work-card, .filter-card { border-radius: 8px; }
.metric-card :deep(.el-card__body) { display:flex; flex-direction:column; gap:8px; }
.metric-card span, .period-card span { color: var(--his-text-2); font-size:13px; }
.metric-card strong { font-size:26px; color: var(--his-text); }
.period-card { padding: 14px; border: 1px solid rgba(var(--his-primary-rgb), .14); border-radius: 8px; background: rgba(var(--his-primary-rgb), .05); display:flex; flex-direction:column; gap:6px; }
.period-card strong { font-size:18px; color: var(--his-text); }
.portal-tabs :deep(.el-tabs__content) { overflow: visible; }
@media (max-width: 768px) { .staff-header { align-items: flex-start; flex-direction: column; } }
</style>
