<template>
  <div class="page department-container">
    <div class="page-header hero-header">
      <div class="hero-left">
        <div class="hero-title-row">
          <el-icon class="hero-icon"><OfficeBuilding /></el-icon>
          <span class="hero-title">科室信息</span>
        </div>
        <div class="hero-subtitle">浏览科室介绍与在岗医生信息</div>
      </div>
    </div>
    <el-row class="fill-row" :gutter="20">
      <!-- 科室列表 -->
      <el-col :xs="24" :lg="8" class="fill-col">
        <el-card class="fill-card" shadow="hover">
          <template #header>
            <span>科室列表</span>
          </template>
          <div v-loading="deptLoading">
            <div v-for="dept in departments" :key="dept.id" class="dept-item" :class="{ active: selectedDept?.id === dept.id }" @click="selectDept(dept)">
              <el-icon><OfficeBuilding /></el-icon>
              <div class="dept-info">
                <div class="dept-name">{{ dept.name }}</div>
                <div class="dept-desc">{{ dept.description }}</div>
              </div>
            </div>
            <el-empty v-if="departments.length === 0" description="暂无科室数据" />
          </div>
        </el-card>
      </el-col>

      <!-- 科室详情与医生 -->
      <el-col :xs="24" :lg="16" class="fill-col">
        <el-card v-if="selectedDept" class="fill-card" shadow="hover">
          <template #header>
            <div class="header-row">
              <span>{{ selectedDept.name }}</span>
              <el-tag>{{ selectedDept.location }}</el-tag>
            </div>
          </template>
          <p class="dept-description">{{ selectedDept.description }}</p>

          <el-divider>科室医生</el-divider>
          <el-table :data="doctors" v-loading="doctorLoading" stripe>
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="title" label="职称" width="120">
              <template #default="{ row }">
                <el-tag :type="row.title === '主任医师' ? 'danger' : row.title === '副主任医师' ? 'warning' : ''" size="small">
                  {{ row.title }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="gender" label="性别" width="80" />
            <el-table-column prop="age" label="年龄" width="80" />
            <el-table-column prop="specialty" label="擅长" min-width="200" show-overflow-tooltip />
          </el-table>
          <el-empty v-if="doctors.length === 0 && !doctorLoading" description="该科室暂无在岗医生" />
        </el-card>
        <el-card v-else class="fill-card" shadow="hover">
          <el-empty description="请从左侧选择一个科室" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDepartmentList, getDepartmentDoctors, getDepartmentDetail } from '@/api/department'
import type { Department, Doctor } from '@/types'
import { OfficeBuilding } from '@element-plus/icons-vue'

const deptLoading = ref(false)
const doctorLoading = ref(false)
const departments = ref<Department[]>([])
const doctors = ref<Doctor[]>([])
const selectedDept = ref<Department | null>(null)

async function loadDepartments() {
  deptLoading.value = true
  try {
    const res = await getDepartmentList()
    departments.value = res.data || []
    if (departments.value.length > 0) {
      selectDept(departments.value[0])
    }
  } catch {
    departments.value = []
  } finally {
    deptLoading.value = false
  }
}

async function selectDept(dept: Department) {
  selectedDept.value = dept
  doctorLoading.value = true
  try {
    const res = await getDepartmentDoctors(dept.id)
    doctors.value = res.data || []
  } catch {
    doctors.value = []
  } finally {
    doctorLoading.value = false
  }
}

onMounted(loadDepartments)
</script>

<style scoped>
.department-container {
  position: relative;
  min-height: calc(100vh - var(--his-header-height));
  background:
    radial-gradient(900px 420px at -8% -10%, rgba(var(--his-primary-rgb), 0.18), transparent 64%),
    radial-gradient(760px 360px at 108% 0%, rgba(var(--his-primary-rgb), 0.12), transparent 68%),
    linear-gradient(160deg, #f4fdf8 0%, #eefaf3 52%, #f8fffb 100%);
}

.department-container .page-header,
.department-container .el-row {
  position: relative;
  z-index: 1;
}

.department-container :deep(.el-card) {
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

.department-container :deep(.el-card__header) {
  border-bottom: 1px solid rgba(var(--his-primary-rgb), 0.14);
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.12), rgba(var(--his-primary-rgb), 0.03));
}

.department-container :deep(.el-card__body) {
  display: flex;
  flex-direction: column;
}

.department-container :deep(.el-table) {
  flex: 1;
  border-radius: 14px;
  overflow: hidden;
}

.department-container :deep(.el-table th.el-table__cell) {
  background: rgba(var(--his-primary-rgb), 0.1);
}

.department-container :deep(.el-table__row:hover > td.el-table__cell) {
  background: rgba(var(--his-primary-rgb), 0.08) !important;
}

.dept-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 12px;
  border-radius: 14px;
  border: 1px solid rgba(var(--his-primary-rgb), 0.14);
  background: rgba(255, 255, 255, 0.76);
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}
.dept-item:hover {
  transform: translateY(-1px);
  background: rgba(var(--his-primary-rgb), 0.08);
  border-color: rgba(var(--his-primary-rgb), 0.34);
  box-shadow: 0 8px 16px rgba(var(--his-primary-rgb), 0.12);
}
.dept-item.active {
  background: linear-gradient(135deg, rgba(var(--his-primary-rgb), 0.16), rgba(var(--his-primary-rgb), 0.06));
  border-color: rgba(var(--his-primary-rgb), 0.44);
}
.dept-info { flex: 1; }
.dept-name { font-weight: 700; color: var(--his-text); }
.dept-desc { font-size: 12px; color: var(--his-text-2); margin-top: 2px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
.dept-description{
  color: var(--his-text-2);
  margin-bottom: 18px;
  line-height: 1.7;
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
}
</style>
