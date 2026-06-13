// ==================== 通用类型 ====================

/** 统一响应格式 */
export interface Result<T = any> {
  success: boolean
  errorMsg: string | null
  data: T
  total: number | null
}

/** 分页参数 */
export interface PageParams {
  page?: number
  size?: number
}

// ==================== 患者账户模块 ====================

/** 注册请求 */
export interface RegisterDto {
  name: string
  username: string
  password: string
  confirmPassword: string
  gender: string
  age: number
  phone: string
  address: string
  idCard: string
}

/** 登录请求 */
export interface LoginDto {
  phone: string
  password: string
}

export type StaffRole = 'patient' | 'doctor' | 'pharmacist' | 'admin'

export interface StaffLoginDto {
  account: string
  password: string
  role: Exclude<StaffRole, 'patient'> | string
}

export interface LoginResponseDto {
  token: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  patientInfo: PatientInfo
}

/** 修改个人信息请求 */
export interface UpdateProfileDto {
  username?: string
  phone?: string
  address?: string
  password: string
}

/** 忘记密码请求 */
export interface ForgetPasswordDto {
  phone: string
  verificationCode: string
  newPassword: string
  confirmPassword: string
}

/** 患者信息 */
export interface PatientInfo {
  account: string
  username: string
  role?: StaffRole | string
  name: string
  gender: string
  age: number
  phone: string
  address: string
  idCard: string
  idCardVerified: boolean
  avatar: string | null
  totalVisits: number
  lastVisitDate: string | null
}

// ==================== 患者信息模块 ====================

/** 患者列表查询参数 */
export interface PatientListParams extends PageParams {
  keyword?: string
}

/** 病历记录 */
export interface MedicalRecord {
  id: number
  patientId: string
  patientName: string
  doctorId: number
  doctorName: string
  departmentId: number
  departmentName: string
  chiefComplaint: string
  presentIllness: string
  diagnosis: string
  treatmentPlan: string
  visitDate: string
  status: number
}

/** 病历详情（含处方） */
export interface MedicalRecordDetail {
  record: MedicalRecord
  prescription: Prescription | null
  prescriptionItems: PrescriptionItem[]
}

/** 处方 */
export interface Prescription {
  id: number
  prescriptionDate: string
  status: number
}

/** 处方明细 */
export interface PrescriptionItem {
  id: number
  medicineName: string
  dosage: string
  quantity: number
  days: number
}

/** 就诊历史查询参数 */
export interface VisitHistoryParams extends PageParams {
  departmentId?: number
  doctorId?: number
  startDate?: string
  endDate?: string
}

/** 就诊记录 */
export interface VisitRecord {
  recordId: number
  visitDate: string
  doctorName: string
  departmentName: string
  diagnosis: string
  prescriptionSummary: string
}

// ==================== 预约模块 ====================

/** 创建预约请求 */
export interface AppointmentCreateDto {
  departmentId: number
  doctorId?: number
  appointmentType: 'DOCTOR' | 'EXAMINATION'
  examinationType?: string
  appointmentDate: string
  timeSlot: string
}

export interface FrontDeskRegistrationDto extends AppointmentCreateDto {
  appointmentId?: number
  patientId: string
  location?: string
}

/** 预约查询参数 */
export interface AppointmentQueryParams extends PageParams {
  status?: number
  departmentId?: number
  doctorId?: number
  startDate?: string
  endDate?: string
}

/** 预约信息 */
export interface Appointment {
  id: number
  patientId: string
  patientName: string
  doctorId: number | null
  doctorName: string
  departmentId: number
  departmentName: string
  appointmentType: string
  examinationType: string | null
  appointmentDate: string
  timeSlot: string
  status: number
  cancelReason: string | null
  aiRecommended: number
  createTime: string
}

/** AI预约推荐请求 */
export interface AiRecommendDto {
  symptom: string
}

/** AI预约推荐结果 */
export interface AppointmentRecommendation {
  department: string
  departmentId?: number
  doctor?: string
  doctorId?: number
  recommendedTime?: string
  recommendedDate?: string
  reason: string
  availableDoctors?: DoctorWithSchedule[]
  needChooseDoctor?: boolean
}

/** 医生及其可用排班（导诊推荐返回） */
export interface DoctorWithSchedule {
  doctorId: number
  doctorName: string
  title: string
  specialty: string
  schedules: DoctorSchedule[]
}

/** 导诊对话消息 */
export interface ChatMessageDto {
  role: 'user' | 'assistant'
  content: string
}

/** 导诊对话请求 */
export interface TriageChatRequest {
  message: string
  history: ChatMessageDto[]
}

/** 导诊对话响应 */
export interface TriageChatResponse {
  reply: string
  completed: boolean
  recommendation?: AppointmentRecommendation
}

/** 排班查询参数 */
export interface ScheduleQueryParams {
  departmentId?: number
  doctorId?: number
  date?: string
}

/** 医生排班 */
export interface DoctorSchedule {
  id: number
  doctorId: number
  doctorName: string
  departmentId: number
  departmentName: string
  scheduleDate: string
  timeSlot: string
  maxPatients: number
  bookedCount: number
  status: number
}

// ==================== 药品模块 ====================

/** 药品列表查询参数 */
export interface MedicineListParams extends PageParams {
  keyword?: string
  category?: string
}

/** 药品信息 */
export interface Medicine {
  id: number
  name: string
  genericName: string
  category: string
  specification: string
  manufacturer: string
  ingredients: string
  efficacy: string
  sideEffects: string
  contraindications: string
  dosage: string
  price: number
  stock: number
  status: number
}

/** AI药品推荐请求 */
export interface AiMedicineRecommendDto {
  symptom: string
}

/** AI药品推荐结果 */
export interface MedicineRecommendation {
  medicineName: string
  reason: string
  dosage: string
  precautions: string
}

/** AI药品推荐对话响应 */
export interface MedicineChatResponse {
  reply: string
  completed: boolean
  recommendations?: MedicineRecommendation[]
}

/** AI费用解释对话响应 */
export interface BillingChatResponse {
  reply: string
  completed: boolean
  explanation?: BillingExplanation
}

// ==================== 费用模块 ====================

/** 费用列表查询参数 */
export interface BillingQueryParams extends PageParams {
  itemType?: 'REGISTRATION' | 'EXAMINATION' | 'MEDICINE' | 'OTHER'
  status?: number
  startDate?: string
  endDate?: string
}

/** 费用信息 */
export interface Billing {
  id: number
  patientId: string
  patientName: string
  itemType: string
  itemName: string
  amount: number
  description: string
  status: number
  createTime: string
}

/** AI费用解释请求 */
export interface AiExplainDto {
  question: string
  startDate?: string
  endDate?: string
}

/** AI费用解释结果 */
export interface BillingExplanation {
  totalAmount: number
  breakdown: string
  explanation: string
  suggestion: string
}

export interface BillingTypeSummaryItem {
  itemType: 'REGISTRATION' | 'EXAMINATION' | 'MEDICINE' | 'OTHER' | string
  amount: number
  count: number
}

export interface BillingTypeSummary {
  totalAmount: number
  items: BillingTypeSummaryItem[]
}

// ==================== 医疗报告模块 ====================

/** 报告生成请求 */
export interface ReportGenerateDto {
  medicalRecordId?: number
  reportType: 'EXAMINATION' | 'TREATMENT'
  title: string
  examinationData: string
}

/** 医疗报告 */
export interface MedicalReport {
  id: number
  patientId: string
  patientName: string
  medicalRecordId: number | null
  doctorId: number | null
  doctorName: string
  reportType: string
  title: string
  examinationData: string | null
  aiSummary: string | null
  aiDiagnosis: string | null
  aiTreatment: string | null
  aiRecommendation: string | null
  aiThoughtChain: string | null
  pdfPath: string | null
  status: number
  createTime: string
}

/** 报告列表查询参数 */
export interface ReportListParams extends PageParams {
  reportType?: 'EXAMINATION' | 'TREATMENT'
}

// ==================== 科室模块 ====================

/** 科室信息 */
export interface Department {
  id: number
  name: string
  description: string
  location: string
  status: number
}

/** 医生信息 */
export interface Doctor {
  id: number
  name: string
  gender: string
  age: number
  title: string
  departmentId: number
  specialty: string
  phone: string
  status: number
}

// ==================== 异步任务模块 ====================

/** 异步任务结果 */
export interface AsyncTaskResult {
  taskId: string
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED' | 'NOT_FOUND'
  resultJson: string | null
  errorMsg: string | null
  createTime: number
  finishTime: number | null
}

// ==================== Staff portals ====================

export interface DoctorCallPatientDto {
  appointmentId: number
  location?: string
}

export interface VisitRecordDto {
  appointmentId: number
  chiefComplaint?: string
  presentIllness?: string
  diagnosis?: string
  treatment?: string
  notes?: string
  prescriptionItems?: PrescriptionItemCreateDto[]
}

export interface PrescriptionItemCreateDto {
  medicineId: number
  dosage?: string
  quantity: number
  days?: number
  remark?: string
}

export interface PrescriptionRecord {
  id: number
  medicalRecordId: number
  patientId: string
  doctorId: number
  prescriptionDate: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface PrescriptionAuditDto {
  prescriptionId: number
  auditStatus: number
  auditRemark?: string
}

export interface MedicineInventory {
  id: number
  medicineId: number
  medicineName?: string
  quantity: number
  minStock: number
  maxStock: number
  purchasePrice?: number
  sellingPrice?: number
  supplier?: string
  batchNumber?: string
  expiryDate?: string
  createTime?: string
  updateTime?: string
}

export interface MedicineInventoryDto {
  medicineId: number
  quantity: number
  minStock?: number
  maxStock?: number
  purchasePrice?: number
  sellingPrice?: number
  supplier?: string
  batchNumber?: string
  expiryDate?: string
}

export interface MedicineStockLog {
  id: number
  medicineId: number
  inventoryId: number
  operationType: string
  quantity: number
  beforeStock: number
  afterStock: number
  unitPrice?: number
  operator?: string
  remark?: string
  createTime: string
}

export interface StatisticsQueryParams {
  startDate?: string
  endDate?: string
  departmentId?: number
  doctorId?: number
  statisticsType?: string
}

export interface AdminStatistics {
  totalAppointments?: number
  completedAppointments?: number
  cancelledAppointments?: number
  totalRevenue?: number
  paidAmount?: number
  newPatients?: number
  totalPatients?: number
}

export interface AdminGlobalStatistics {
  totalPatients?: number
  totalDoctors?: number
  totalDepartments?: number
  totalMedicines?: number
  todayAppointments?: number
  unpaidBills?: number
}

export interface SystemConfig {
  id: number
  configKey: string
  configValue: string
  configType?: string
  description?: string
}

export interface SystemConfigDto extends SystemConfig {}

export interface AdminUserManagement {
  patients: PatientInfo[]
  doctors: Doctor[]
  pharmacists: Array<Record<string, any>>
}

export interface AdminScheduleManagement {
  doctors: Doctor[]
  departments: Department[]
}
