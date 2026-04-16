import { get, post } from './request'
import type {
  Medicine,
  MedicineListParams,
  AiMedicineRecommendDto,
  MedicineRecommendation,
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
