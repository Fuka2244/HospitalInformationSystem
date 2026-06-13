import { get, put } from './request'
import type {
  AdminGlobalStatistics,
  AdminScheduleManagement,
  AdminStatistics,
  AdminUserManagement,
  Department,
  StatisticsQueryParams,
  SystemConfig,
  SystemConfigDto,
} from '@/types'

export function getStatistics(params: StatisticsQueryParams) {
  return get<AdminStatistics>('/admin/statistics', params)
}

export function getGlobalStatistics() {
  return get<AdminGlobalStatistics>('/admin/global-statistics')
}

export function getPeriodicReport(params: StatisticsQueryParams) {
  return get<Record<string, any>>('/admin/periodic-report', params)
}

export function getSystemConfigs() {
  return get<SystemConfig[]>('/admin/system-configs')
}

export function updateSystemConfig(data: SystemConfigDto) {
  return put<string>('/admin/system-config', data)
}

export function getUserManagement() {
  return get<AdminUserManagement>('/admin/user-management')
}

export function getDepartmentManagement() {
  return get<Department[]>('/admin/department-management')
}

export function getDoctorScheduleManagement() {
  return get<AdminScheduleManagement>('/admin/doctor-schedule-management')
}
