import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { title: '登录', public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/home/HomeView.vue'), meta: { title: '首页', public: true } },
      { path: 'project-tech', name: 'ProjectTech', component: () => import('@/views/home/ProjectTechView.vue'), meta: { title: '项目技术', public: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/patient/ProfileView.vue'), meta: { title: '个人信息' } },
      { path: 'records', name: 'Records', component: () => import('@/views/patient/RecordsView.vue'), meta: { title: '病历与就诊' } },
      { path: 'appointment', name: 'Appointment', component: () => import('@/views/appointment/AppointmentView.vue'), meta: { title: '预约挂号' } },
      { path: 'medicine', name: 'Medicine', component: () => import('@/views/medicine/MedicineView.vue'), meta: { title: '药品查询', public: true } },
      { path: 'billing', name: 'Billing', component: () => import('@/views/billing/BillingView.vue'), meta: { title: '费用查询' } },
      { path: 'report', name: 'Report', component: () => import('@/views/report/ReportView.vue'), meta: { title: '医疗报告' } },
      { path: 'department', name: 'Department', component: () => import('@/views/department/DepartmentView.vue'), meta: { title: '科室信息', public: true } },
      { path: 'about', name: 'About', component: () => import('@/views/about/AboutView.vue'), meta: { title: '系统概况', public: true } },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 }
  },
})

// 路由守卫
router.beforeEach(async (to, _from, next) => {
  document.title = `${to.meta.title || 'HIS'} - 医院信息系统`

  const userStore = useUserStore()

  // 公开页面直接放行
  if (to.meta.public) {
    // 已登录用户访问登录页时跳转首页
    if (to.path === '/login' && userStore.isLoggedIn) {
      next('/')
      return
    }
    next()
    return
  }

  // 需要登录的页面：检查登录状态
  if (userStore.isLoggedIn) {
    next()
    return
  }

  // 尝试从后端恢复会话
  try {
    await userStore.fetchProfile()
    if (userStore.isLoggedIn) {
      next()
    } else {
      // 未登录，跳转到登录页，并记录原始目标路径
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  } catch {
    // 后端不可用或未登录，跳转登录页
    next({ path: '/login', query: { redirect: to.fullPath } })
  }
})

export default router
