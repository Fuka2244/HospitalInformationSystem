import { get, post, put } from './request'
import type {
  MedicineInventory,
  MedicineInventoryDto,
  MedicineStockLog,
  PrescriptionAuditDto,
  PrescriptionRecord,
} from '@/types'

export function getPendingPrescriptions() {
  return get<PrescriptionRecord[]>('/pharmacist/pending-prescriptions')
}

export function auditPrescription(data: PrescriptionAuditDto) {
  return post<string>('/pharmacist/audit-prescription', data)
}

export function dispenseMedicine(prescriptionId: number) {
  return put<string>(`/pharmacist/dispense-medicine/${prescriptionId}`)
}

export function getMedicineInventory() {
  return get<MedicineInventory[]>('/pharmacist/medicine-inventory')
}

export function addMedicineInventory(data: MedicineInventoryDto) {
  return post<string>('/pharmacist/medicine-inventory', data)
}

export function updateMedicineInventory(inventoryId: number, data: MedicineInventoryDto) {
  return put<string>(`/pharmacist/medicine-inventory/${inventoryId}`, data)
}

export function getLowStockMedicines() {
  return get<MedicineInventory[]>('/pharmacist/low-stock-medicines')
}

export function getStockLogs(medicineId?: number) {
  return get<MedicineStockLog[]>('/pharmacist/stock-logs', { medicineId })
}
