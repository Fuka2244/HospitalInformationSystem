import { get, post } from './request'
import type {
  Billing,
  BillingQueryParams,
  AiExplainDto,
  BillingExplanation,
  BillingChatResponse,
  ChatMessageDto,
} from '@/types'

/** 当前用户费用列表 */
export function getBillingList(params: BillingQueryParams) {
  return get<Billing[]>('/billing/list', params)
}

/** 指定患者费用列表 */
export function getPatientBillingList(patientId: string, params: BillingQueryParams) {
  return get<Billing[]>(`/billing/${patientId}/list`, params)
}

/** 费用详情 */
export function getBillingDetail(id: number) {
  return get<Billing>(`/billing/detail/${id}`)
}

/** AI费用解释 */
export function aiExplainBilling(data: AiExplainDto) {
  return post<BillingExplanation>('/billing/ai-explain', data)
}

/** AI多轮对话式费用解释 */
export function aiBillingChat(data: { message: string; startDate?: string; endDate?: string; history: ChatMessageDto[] }) {
  return post<BillingChatResponse>('/billing/ai-chat', data)
}
