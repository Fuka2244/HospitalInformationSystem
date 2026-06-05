package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.dto.MedicineInventoryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.PrescriptionAuditDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.MedicineInventory;
import com.hospitalinfo.hospitalinformationsystem.entity.MedicineStockLog;
import com.hospitalinfo.hospitalinformationsystem.entity.Prescription;
import com.hospitalinfo.hospitalinformationsystem.entity.PrescriptionAudit;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineInventoryMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineStockLogMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.PrescriptionAuditMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.PrescriptionMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IPharmacistService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PharmacistServiceImpl implements IPharmacistService {

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionAuditMapper prescriptionAuditMapper;
    private final MedicineInventoryMapper medicineInventoryMapper;
    private final MedicineStockLogMapper medicineStockLogMapper;

    @Override
    public Result getPendingPrescriptions(HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"pharmacist".equals(role) && !"admin".equals(role)) {
            return Result.fail("无权访问药师功能");
        }

        QueryWrapper<Prescription> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 0)
               .orderByAsc("prescription_date");

        List<Prescription> prescriptions = prescriptionMapper.selectList(wrapper);
        return Result.ok(prescriptions);
    }

    @Override
    @Transactional
    public Result auditPrescription(PrescriptionAuditDto dto, HttpSession session) {
        String pharmacistId = (String) session.getAttribute("account");
        String role = (String) session.getAttribute("role");
        
        if (!"pharmacist".equals(role)) {
            return Result.fail("无权执行此操作");
        }

        Prescription prescription = prescriptionMapper.selectById(dto.getPrescriptionId());
        if (prescription == null) {
            return Result.fail("处方不存在");
        }

        if (prescription.getStatus() != 0) {
            return Result.fail("该处方已审核");
        }

        prescription.setStatus(dto.getAuditStatus());
        prescription.setUpdateTime(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);

        PrescriptionAudit audit = new PrescriptionAudit();
        audit.setPrescriptionId(dto.getPrescriptionId());
        audit.setPharmacistId(Long.parseLong(pharmacistId));
        audit.setAuditStatus(dto.getAuditStatus());
        audit.setAuditRemark(dto.getAuditRemark());
        audit.setAuditTime(LocalDateTime.now());
        audit.setCreateTime(LocalDateTime.now());
        audit.setUpdateTime(LocalDateTime.now());
        prescriptionAuditMapper.insert(audit);

        return Result.ok("审核完成");
    }

    @Override
    @Transactional
    public Result dispenseMedicine(Long prescriptionId, HttpSession session) {
        String pharmacistId = (String) session.getAttribute("account");
        String role = (String) session.getAttribute("role");
        
        if (!"pharmacist".equals(role)) {
            return Result.fail("无权执行此操作");
        }

        Prescription prescription = prescriptionMapper.selectById(prescriptionId);
        if (prescription == null) {
            return Result.fail("处方不存在");
        }

        if (prescription.getStatus() != 1) {
            return Result.fail("该处方未通过审核");
        }

        prescription.setStatus(2);
        prescription.setUpdateTime(LocalDateTime.now());
        prescriptionMapper.updateById(prescription);

        return Result.ok("发药完成");
    }

    @Override
    public Result getMedicineInventory(HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"pharmacist".equals(role) && !"admin".equals(role)) {
            return Result.fail("无权访问库存管理");
        }

        QueryWrapper<MedicineInventory> wrapper = new QueryWrapper<>();
        List<MedicineInventory> inventoryList = medicineInventoryMapper.selectList(wrapper);
        return Result.ok(inventoryList);
    }

    @Override
    @Transactional
    public Result addMedicineInventory(MedicineInventoryDto dto, HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"pharmacist".equals(role) && !"admin".equals(role)) {
            return Result.fail("无权执行此操作");
        }

        MedicineInventory inventory = new MedicineInventory();
        inventory.setMedicineId(dto.getMedicineId());
        inventory.setQuantity(dto.getQuantity());
        inventory.setMinStock(dto.getMinStock() != null ? dto.getMinStock() : 10);
        inventory.setMaxStock(dto.getMaxStock() != null ? dto.getMaxStock() : 1000);
        inventory.setPurchasePrice(dto.getPurchasePrice());
        inventory.setSellingPrice(dto.getSellingPrice());
        inventory.setSupplier(dto.getSupplier());
        inventory.setBatchNumber(dto.getBatchNumber());
        inventory.setExpiryDate(dto.getExpiryDate());
        inventory.setCreateTime(LocalDateTime.now());
        inventory.setUpdateTime(LocalDateTime.now());
        medicineInventoryMapper.insert(inventory);

        MedicineStockLog log = new MedicineStockLog();
        log.setMedicineId(dto.getMedicineId());
        log.setInventoryId(inventory.getId());
        log.setOperationType("入库");
        log.setQuantity(dto.getQuantity());
        log.setBeforeStock(0);
        log.setAfterStock(dto.getQuantity());
        log.setUnitPrice(dto.getPurchasePrice());
        log.setOperator((String) session.getAttribute("account"));
        log.setRemark("初始入库");
        log.setCreateTime(LocalDateTime.now());
        medicineStockLogMapper.insert(log);

        return Result.ok("入库成功");
    }

    @Override
    @Transactional
    public Result updateMedicineInventory(Long inventoryId, MedicineInventoryDto dto, HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"pharmacist".equals(role) && !"admin".equals(role)) {
            return Result.fail("无权执行此操作");
        }

        MedicineInventory inventory = medicineInventoryMapper.selectById(inventoryId);
        if (inventory == null) {
            return Result.fail("库存记录不存在");
        }

        Integer beforeStock = inventory.getQuantity();
        inventory.setQuantity(dto.getQuantity());
        inventory.setMinStock(dto.getMinStock());
        inventory.setMaxStock(dto.getMaxStock());
        inventory.setPurchasePrice(dto.getPurchasePrice());
        inventory.setSellingPrice(dto.getSellingPrice());
        inventory.setSupplier(dto.getSupplier());
        inventory.setBatchNumber(dto.getBatchNumber());
        inventory.setExpiryDate(dto.getExpiryDate());
        inventory.setUpdateTime(LocalDateTime.now());
        medicineInventoryMapper.updateById(inventory);

        MedicineStockLog log = new MedicineStockLog();
        log.setMedicineId(inventory.getMedicineId());
        log.setInventoryId(inventoryId);
        log.setOperationType("盘点调整");
        log.setQuantity(dto.getQuantity() - beforeStock);
        log.setBeforeStock(beforeStock);
        log.setAfterStock(dto.getQuantity());
        log.setUnitPrice(dto.getPurchasePrice());
        log.setOperator((String) session.getAttribute("account"));
        log.setRemark("库存盘点调整");
        log.setCreateTime(LocalDateTime.now());
        medicineStockLogMapper.insert(log);

        return Result.ok("更新成功");
    }

    @Override
    public Result getLowStockMedicines(HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"pharmacist".equals(role) && !"admin".equals(role)) {
            return Result.fail("无权访问库存预警");
        }

        QueryWrapper<MedicineInventory> wrapper = new QueryWrapper<>();
        wrapper.apply("quantity <= min_stock");
        List<MedicineInventory> lowStockList = medicineInventoryMapper.selectList(wrapper);
        return Result.ok(lowStockList);
    }

    @Override
    public Result getStockLogs(Long medicineId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"pharmacist".equals(role) && !"admin".equals(role)) {
            return Result.fail("无权查看库存日志");
        }

        QueryWrapper<MedicineStockLog> wrapper = new QueryWrapper<>();
        if (medicineId != null) {
            wrapper.eq("medicine_id", medicineId);
        }
        wrapper.orderByDesc("create_time");
        List<MedicineStockLog> logs = medicineStockLogMapper.selectList(wrapper);
        return Result.ok(logs);
    }
}