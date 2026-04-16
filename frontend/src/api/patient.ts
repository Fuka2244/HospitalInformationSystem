import { get, post, put } from './request'
import type {
  LoginDto,
  LoginVo,
  RegisterDto,
  UpdateProfileDto,
  ForgetPasswordDto,
  PatientInfo,
  PatientListParams,
  MedicalRecordDetail,
  VisitHistoryParams,
  VisitRecord,
} from '@/types'

// ===== 账户管理 =====

/** 用户注册 */
export function register(data: RegisterDto) {
  return post<LoginVo>('/patient/register', data)
}

/** 用户登录 */
export function login(data: LoginDto) {
  return post<LoginVo>('/patient/login', data)
}

/** 用户登出 */
export function logout() {
  return post('/patient/loginout')
}

/** 查看个人信息 */
export function getProfile() {
  return get<PatientInfo>('/patient/me')
}

/** 修改个人信息 */
export function updateProfile(data: UpdateProfileDto) {
  return put('/patient/me/update', data)
}

/** 忘记密码 */
export function forgetPassword(data: ForgetPasswordDto) {
  return put('/patient/login/forget', data)
}

// ===== 患者信息管理 =====

/** 当前患者基本信息 */
export function getPatientInfo() {
  return get<PatientInfo>('/patient/info')
}

/** 指定患者信息 */
export function getPatientById(patientId: string) {
  return get<PatientInfo>(`/patient/${patientId}`)
}

/** 患者列表 */
export function getPatientList(params: PatientListParams) {
  return get<PatientInfo[]>('/patient/list', params)
}

/** 当前患者病历列表 */
export function getMedicalRecords(params: { page?: number; size?: number }) {
  return get<any[]>('/patient/medical-records', params)
}

/** 指定患者病历列表 */
export function getPatientMedicalRecords(patientId: string, params: { page?: number; size?: number }) {
  return get<any[]>(`/patient/${patientId}/medical-records`, params)
}

/** 病历详情 */
export function getMedicalRecordDetail(recordId: number) {
  return get<MedicalRecordDetail>(`/patient/medical-record/${recordId}`)
}

/** 历史就诊记录 */
export function getVisitHistory(params: VisitHistoryParams) {
  return get<VisitRecord[]>('/patient/visit-history', params)
}
