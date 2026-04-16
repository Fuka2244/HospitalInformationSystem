import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { PatientInfo, LoginDto, RegisterDto } from '@/types'
import * as patientApi from '@/api/patient'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<PatientInfo | null>(null)
  const isLoggedIn = computed(() => !!userInfo.value)

  /** 登录 */
  async function login(dto: LoginDto) {
    const res = await patientApi.login(dto)
    // 登录后拉取完整信息
    await fetchProfile()
    return res
  }

  /** 注册 */
  async function register(dto: RegisterDto) {
    const res = await patientApi.register(dto)
    return res
  }

  /** 登出 */
  async function logout() {
    await patientApi.logout()
    userInfo.value = null
  }

  /** 获取个人信息 */
  async function fetchProfile() {
    try {
      const res = await patientApi.getProfile()
      userInfo.value = res.data
    } catch {
      userInfo.value = null
    }
  }

  return { userInfo, isLoggedIn, login, register, logout, fetchProfile }
})
