package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.MedicineInventoryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.PrescriptionAuditDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import jakarta.servlet.http.HttpSession;

public interface IPharmacistService {
    Result getPendingPrescriptions(HttpSession session);
    Result auditPrescription(PrescriptionAuditDto dto, HttpSession session);
    Result dispenseMedicine(Long prescriptionId, HttpSession session);
    Result getMedicineInventory(HttpSession session);
    Result addMedicineInventory(MedicineInventoryDto dto, HttpSession session);
    Result updateMedicineInventory(Long inventoryId, MedicineInventoryDto dto, HttpSession session);
    Result getLowStockMedicines(HttpSession session);
    Result getStockLogs(Long medicineId, HttpSession session);
}