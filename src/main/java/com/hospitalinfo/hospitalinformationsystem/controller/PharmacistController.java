package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.MedicineInventoryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.PrescriptionAuditDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.service.IPharmacistService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pharmacist")
@RequiredArgsConstructor
public class PharmacistController {

    private final IPharmacistService pharmacistService;

    @GetMapping("/pending-prescriptions")
    public Result getPendingPrescriptions(HttpSession session) {
        return pharmacistService.getPendingPrescriptions(session);
    }

    @PostMapping("/audit-prescription")
    public Result auditPrescription(@RequestBody @Valid PrescriptionAuditDto dto, HttpSession session) {
        return pharmacistService.auditPrescription(dto, session);
    }

    @PutMapping("/dispense-medicine/{prescriptionId}")
    public Result dispenseMedicine(@PathVariable Long prescriptionId, HttpSession session) {
        return pharmacistService.dispenseMedicine(prescriptionId, session);
    }

    @GetMapping("/medicine-inventory")
    public Result getMedicineInventory(HttpSession session) {
        return pharmacistService.getMedicineInventory(session);
    }

    @PostMapping("/medicine-inventory")
    public Result addMedicineInventory(@RequestBody @Valid MedicineInventoryDto dto, HttpSession session) {
        return pharmacistService.addMedicineInventory(dto, session);
    }

    @PutMapping("/medicine-inventory/{inventoryId}")
    public Result updateMedicineInventory(@PathVariable Long inventoryId, @RequestBody MedicineInventoryDto dto, HttpSession session) {
        return pharmacistService.updateMedicineInventory(inventoryId, dto, session);
    }

    @GetMapping("/low-stock-medicines")
    public Result getLowStockMedicines(HttpSession session) {
        return pharmacistService.getLowStockMedicines(session);
    }

    @GetMapping("/stock-logs")
    public Result getStockLogs(@RequestParam(required = false) Long medicineId, HttpSession session) {
        return pharmacistService.getStockLogs(medicineId, session);
    }
}