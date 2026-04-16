<template>
  <div class="department-container" style="padding: 20px">
    <el-row :gutter="20">
      <!-- 科室列表 -->
      <el-col :span="8">
        <el-card shadow="hover">
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
      <el-col :span="16">
        <el-card v-if="selectedDept" shadow="hover">
          <template #header>
            <div class="header-row">
              <span>{{ selectedDept.name }}</span>
              <el-tag>{{ selectedDept.location }}</el-tag>
            </div>
          </template>
          <p style="color: #606266; margin-bottom: 20px">{{ selectedDept.description }}</p>

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
        <el-card v-else shadow="hover">
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
  } finally {
    doctorLoading.value = false
  }
}

onMounted(loadDepartments)
</script>

<style scoped>
.dept-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}
.dept-item:hover { background: #f5f7fa; }
.dept-item.active { background: #ecf5ff; }
.dept-info { flex: 1; }
.dept-name { font-weight: bold; color: #303133; }
.dept-desc { font-size: 12px; color: #909399; margin-top: 2px; }
.header-row { display: flex; justify-content: space-between; align-items: center; }
</style>
