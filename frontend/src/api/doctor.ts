import { get, post, put } from './request'
import type { Appointment, DoctorCallPatientDto, MedicalRecord, VisitRecordDto } from '@/types'

export function getTodayAppointments() {
  return get<Appointment[]>('/doctor/today-appointments')
}

export function callPatient(data: DoctorCallPatientDto) {
  return post<string>('/doctor/call-patient', data)
}

export function startVisit(appointmentId: number) {
  return put<string>(`/doctor/start-visit/${appointmentId}`)
}

export function endVisit(appointmentId: number, data: VisitRecordDto) {
  return put<MedicalRecord>(`/doctor/end-visit/${appointmentId}`, data)
}

export function getPatientMedicalRecords(patientId: string) {
  return get<MedicalRecord[]>(`/doctor/patient/${patientId}/medical-records`)
}
