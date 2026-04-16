import { get } from './request'
import type { Department, Doctor } from '@/types'

/** 科室列表 */
export function getDepartmentList() {
  return get<Department[]>('/department/list')
}

/** 科室详情 */
export function getDepartmentDetail(id: number) {
  return get<Department>(`/department/${id}`)
}

/** 科室下医生列表 */
export function getDepartmentDoctors(id: number) {
  return get<Doctor[]>(`/department/${id}/doctors`)
}
