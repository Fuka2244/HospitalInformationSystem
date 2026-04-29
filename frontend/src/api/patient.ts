import { get, post, put, upload } from './request'
import type {
  LoginDto,
  RegisterDto,
  UpdateProfileDto,
  ForgetPasswordDto,
  PatientInfo,
  PatientListParams,
  MedicalRecord,
  MedicalRecordDetail,
  VisitHistoryParams,
  VisitRecord,
} from '@/types'

// ===== 账户管理 =====

/** 用户注册 */
export function register(data: RegisterDto) {
  return post<PatientInfo>('/patient/register', data)
}

/** 用户登录 */
export function login(data: LoginDto) {
  return post<PatientInfo>('/patient/login', data)
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

/** 发送验证码（忘记密码用） */
export function sendVerificationCode(phone: string) {
  return post('/patient/login/send-code', { phone })
}

/** 忘记密码 */
export function forgetPassword(data: ForgetPasswordDto) {
  return put('/patient/login/forget', data)
}

/** 验证密码后获取完整身份证号 */
export function getIdCard(password: string) {
  return post<{ idCard: string }>('/patient/id-card', { password })
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
  return get<MedicalRecord[]>('/patient/medical-records', params)
}

/** 指定患者病历列表 */
export function getPatientMedicalRecords(patientId: string, params: { page?: number; size?: number }) {
  return get<MedicalRecord[]>(`/patient/${patientId}/medical-records`, params)
}

/** 病历详情 */
export function getMedicalRecordDetail(recordId: number) {
  return get<MedicalRecordDetail>(`/patient/medical-record/${recordId}`)
}

/** 历史就诊记录 */
export function getVisitHistory(params: VisitHistoryParams) {
  return get<VisitRecord[]>('/patient/visit-history', params)
}

// ===== 头像管理 =====

/** 上传头像 */
export function uploadAvatar(file: File) {
  return upload<string>('/file/avatar', file)
}

/** 获取当前用户头像URL */
export function getAvatar() {
  return get<string>('/file/avatar')
}
