import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Appointment, AppointmentCreateDto, AppointmentQueryParams, AppointmentRecommendation, ChatMessageDto, DoctorSchedule, TriageChatResponse } from '@/types'
import * as appointmentApi from '@/api/appointment'

export const useAppointmentStore = defineStore('appointment', () => {
  const appointments = ref<Appointment[]>([])
  const total = ref(0)
  const loading = ref(false)
  const schedules = ref<DoctorSchedule[]>([])
  const aiRecommendation = ref<AppointmentRecommendation | null>(null)
  const aiAvailableSchedules = ref<DoctorSchedule[]>([])

  /** 获取预约列表 */
  async function fetchList(params: AppointmentQueryParams) {
    loading.value = true
    try {
      const res = await appointmentApi.getAppointmentList(params)
      appointments.value = res.data || []
      total.value = res.total || 0
    } finally {
      loading.value = false
    }
  }

  /** 创建预约 */
  async function create(data: AppointmentCreateDto) {
    return appointmentApi.createAppointment(data)
  }

  /** 取消预约 */
  async function cancel(id: number, reason: string) {
    return appointmentApi.cancelAppointment(id, reason)
  }

  /** 改期 */
  async function reschedule(id: number, data: AppointmentCreateDto) {
    return appointmentApi.rescheduleAppointment(id, data)
  }

  /** AI推荐 */
  async function aiRecommend(symptom: string) {
    const res = await appointmentApi.aiRecommendAppointment({ symptom })
    aiRecommendation.value = res.data
    return res
  }

  /** AI推荐并查询可用排班 */
  async function aiRecommendWithSchedules(symptom: string) {
    const res = await appointmentApi.aiRecommendWithSchedules({ symptom })
    aiRecommendation.value = res.data.recommendation
    aiAvailableSchedules.value = res.data.availableSchedules || []
    return res
  }

  /** AI多轮对话式导诊 */
  async function aiTriageChat(message: string, history: ChatMessageDto[]): Promise<TriageChatResponse> {
    const res = await appointmentApi.aiTriageChat({ message, history })
    return res.data
  }

  /** 获取排班 */
  async function fetchSchedules(params: { departmentId?: number; doctorId?: number; date?: string }) {
    const res = await appointmentApi.getAvailableSchedules(params)
    schedules.value = res.data || []
  }

  return { appointments, total, loading, schedules, aiRecommendation, aiAvailableSchedules, fetchList, create, cancel, reschedule, aiRecommend, aiRecommendWithSchedules, aiTriageChat, fetchSchedules }
})
