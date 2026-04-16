import { get, post, put } from './request'
import type {
  MedicalReport,
  ReportGenerateDto,
  ReportListParams,
} from '@/types'

/** AI生成医疗报告 */
export function generateReport(data: ReportGenerateDto) {
  return post<MedicalReport>('/report/generate', data)
}

/** 报告列表 */
export function getReportList(params: ReportListParams) {
  return get<MedicalReport[]>('/report/list', params)
}

/** 报告详情 */
export function getReportDetail(id: number) {
  return get<MedicalReport>(`/report/${id}`)
}

/** 导出PDF */
export function exportReportPdf(id: number) {
  return get<string>(`/report/${id}/export-pdf`)
}

/** 确认报告 */
export function confirmReport(id: number) {
  return put(`/report/${id}/confirm`)
}
