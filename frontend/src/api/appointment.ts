import { get, post, put } from './request'
import type {
  Appointment,
  AppointmentCreateDto,
  AppointmentQueryParams,
  AppointmentRecommendation,
  AiRecommendDto,
  DoctorSchedule,
  ScheduleQueryParams,
  TriageChatRequest,
  TriageChatResponse,
} from '@/types'

/** 创建预约 */
export function createAppointment(data: AppointmentCreateDto) {
  return post<Appointment>('/appointment', data)
}

/** 预约列表 */
export function getAppointmentList(params: AppointmentQueryParams) {
  return get<Appointment[]>('/appointment/list', params)
}

/** 预约详情 */
export function getAppointmentDetail(id: number) {
  return get<Appointment>(`/appointment/${id}`)
}

/** 取消预约 */
export function cancelAppointment(id: number, cancelReason: string) {
  return put(`/appointment/${id}/cancel`, null, { params: { cancelReason } })
}

/** 改期预约 */
export function rescheduleAppointment(id: number, data: AppointmentCreateDto) {
  return put<Appointment>(`/appointment/${id}/reschedule`, data)
}

/** AI智能预约推荐 */
export function aiRecommendAppointment(data: AiRecommendDto) {
  return post<AppointmentRecommendation>('/appointment/ai-recommend', data)
}

/** AI智能预约推荐并查询可用排班 */
export function aiRecommendWithSchedules(data: AiRecommendDto) {
  return post<AppointmentRecommendation>('/appointment/ai-recommend-with-schedules', data)
}

/** AI多轮对话式智能导诊 */
export function aiTriageChat(data: TriageChatRequest) {
  return post<TriageChatResponse>('/appointment/ai-triage-chat', data)
}

/** 可用排班查询 */
export function getAvailableSchedules(params: ScheduleQueryParams) {
  return get<DoctorSchedule[]>('/appointment/schedules', params)
}
