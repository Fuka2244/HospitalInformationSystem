import { get, post } from './request'
import type {
  Medicine,
  MedicineListParams,
  AiMedicineRecommendDto,
  MedicineRecommendation,
  MedicineChatResponse,
  ChatMessageDto,
  AsyncTaskResult,
} from '@/types'

/** 药品列表 */
export function getMedicineList(params: MedicineListParams) {
  return get<Medicine[]>('/medicine/list', params)
}

/** 药品详情 */
export function getMedicineDetail(id: number) {
  return get<Medicine>(`/medicine/${id}`)
}

/** AI药品推荐 */
export function aiRecommendMedicine(data: AiMedicineRecommendDto) {
  return post<MedicineRecommendation[]>('/medicine/ai-recommend', data)
}

/** AI多轮对话式药品推荐（同步） */
export function aiMedicineChat(data: { message: string; history: ChatMessageDto[] }) {
  return post<MedicineChatResponse>('/medicine/ai-chat', data)
}

/** AI多轮对话式药品推荐（异步，返回taskId） */
export function aiMedicineChatAsync(data: { message: string; history: ChatMessageDto[] }) {
  return post<string>('/medicine/ai-chat-async', data)
}

/** 查询异步任务结果 */
export function getMedicineAiTaskResult(taskId: string) {
  return get<AsyncTaskResult>(`/medicine/ai-task/${taskId}`)
}
