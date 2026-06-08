import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { LoginDto, LoginResponseDto, PatientInfo, RegisterDto, StaffRole } from '@/types'
import * as patientApi from '@/api/patient'
import * as staffApi from '@/api/staff'

const TOKEN_KEY = 'his_token'
const REFRESH_TOKEN_KEY = 'his_refresh_token'
const ROLE_KEY = 'his_role'
const USER_INFO_KEY = 'his_user_info'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<PatientInfo | null>(null)
  const isLoggedIn = computed(() => !!userInfo.value)
  const currentRole = computed<StaffRole | string>(() => userInfo.value?.role || localStorage.getItem(ROLE_KEY) || 'patient')

  function saveLoginState(data: LoginResponseDto, role: StaffRole | string) {
    localStorage.setItem(TOKEN_KEY, data.token)
    localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken)
    localStorage.setItem(ROLE_KEY, role)

    const nextUser = { ...data.patientInfo, role: data.patientInfo.role || role }
    userInfo.value = nextUser
    localStorage.setItem(USER_INFO_KEY, JSON.stringify(nextUser))
  }

  function clearLoginState() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
    localStorage.removeItem(ROLE_KEY)
    localStorage.removeItem(USER_INFO_KEY)
    userInfo.value = null
  }

  function hydrateFromStorage() {
    const raw = localStorage.getItem(USER_INFO_KEY)
    if (!raw) return false

    try {
      userInfo.value = JSON.parse(raw)
      return !!userInfo.value
    } catch {
      clearLoginState()
      return false
    }
  }

  async function login(dto: LoginDto, role: StaffRole = 'patient') {
    const res = role === 'patient'
      ? await patientApi.login(dto)
      : await staffApi.staffLogin({ account: dto.phone, password: dto.password, role })

    saveLoginState(res.data, role)
    return res
  }

  async function register(dto: RegisterDto) {
    return patientApi.register(dto)
  }

  async function logout() {
    const role = currentRole.value
    try {
      if (role === 'patient') {
        await patientApi.logout()
      } else {
        await staffApi.staffLogout()
      }
    } finally {
      clearLoginState()
    }
  }

  async function fetchProfile() {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      clearLoginState()
      return
    }

    const role = localStorage.getItem(ROLE_KEY) || 'patient'
    if (role !== 'patient') {
      hydrateFromStorage()
      return
    }

    try {
      const res = await patientApi.getProfile()
      userInfo.value = { ...res.data, role: 'patient' }
      localStorage.setItem(USER_INFO_KEY, JSON.stringify(userInfo.value))
    } catch {
      clearLoginState()
    }
  }

  hydrateFromStorage()

  return { userInfo, isLoggedIn, currentRole, login, register, logout, fetchProfile }
})
