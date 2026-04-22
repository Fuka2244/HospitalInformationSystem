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

  if (to.meta.public) {
    next()
    return
  }

  const userStore = useUserStore()
  if (userStore.isLoggedIn) {
    next()
    return
  }

  try {
    await userStore.fetchProfile()
    next()
  } catch {
    // 后端不可用或未登录：不强制跳转登录页，允许浏览公开内容
    // 将需要登录的页面也放行，由各页面内部处理未登录状态
    next()
  }
})

export default router
