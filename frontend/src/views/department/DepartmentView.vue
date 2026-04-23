<template>
  <div class="page department-container">
    <div class="page-header">
      <div>
        <div class="page-title">科室信息</div>
        <div class="page-subtitle">浏览科室介绍与在岗医生信息</div>
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
.department-container :deep(.el-card__body){
  display:flex;
  flex-direction: column;
}
.department-container :deep(.el-table){
  flex: 1;
}
.dept-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(15, 23, 42, 0.06);
  background: rgba(15, 23, 42, 0.02);
  cursor: pointer;
  transition: transform 0.18s ease, background 0.18s ease, border-color 0.18s ease;
}
.dept-item:hover {
  transform: translateY(-1px);
  background: rgba(47, 128, 237, 0.06);
  border-color: rgba(47, 128, 237, 0.18);
}
.dept-item.active {
  background: rgba(47, 128, 237, 0.10);
  border-color: rgba(47, 128, 237, 0.26);
}
.dept-info { flex: 1; }
.dept-name { font-weight: bold; color: #303133; }
.dept-desc { font-size: 12px; color: #909399; margin-top: 2px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
.dept-description{
  color: rgba(15, 23, 42, 0.62);
  margin-bottom: 18px;
  line-height: 1.7;
}
</style>
