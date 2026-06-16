package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.config.CacheConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.DoctorCallPatientDto;
import com.hospitalinfo.hospitalinformationsystem.dto.PrescriptionItemDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.VisitRecordDto;
import com.hospitalinfo.hospitalinformationsystem.entity.Appointment;
import com.hospitalinfo.hospitalinformationsystem.entity.Billing;
import com.hospitalinfo.hospitalinformationsystem.entity.MedicalRecord;
import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import com.hospitalinfo.hospitalinformationsystem.entity.Prescription;
import com.hospitalinfo.hospitalinformationsystem.entity.PrescriptionItem;
import com.hospitalinfo.hospitalinformationsystem.entity.VisitRecord;
import com.hospitalinfo.hospitalinformationsystem.mapper.AppointmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.BillingMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicalRecordMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.PrescriptionItemMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.PrescriptionMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.VisitRecordMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IDoctorService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements IDoctorService {

    private static final BigDecimal DISPENSE_SERVICE_FEE = new BigDecimal("5.00");

    private final AppointmentMapper appointmentMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final VisitRecordMapper visitRecordMapper;
    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final MedicineMapper medicineMapper;
    private final BillingMapper billingMapper;

    @Override
    public Result getTodayAppointments(HttpSession session) {
        String doctorId = (String) session.getAttribute("account");
        String role = (String) session.getAttribute("role");
        
        if (!"doctor".equals(role)) {
            return Result.fail("无权访问医生功能");
        }

        QueryWrapper<Appointment> wrapper = new QueryWrapper<>();
        wrapper.eq("doctor_id", doctorId)
               .eq("appointment_date", LocalDate.now())
               .in("status", 0, 1, 2)
               .orderByAsc("time_slot");

        List<Appointment> appointments = appointmentMapper.selectList(wrapper);
        return Result.ok(appointments);
    }

    @Override
    @Transactional
    public Result callPatient(DoctorCallPatientDto dto, HttpSession session) {
        String doctorId = (String) session.getAttribute("account");
        String role = (String) session.getAttribute("role");
        
        if (!"doctor".equals(role)) {
            return Result.fail("无权执行此操作");
        }

        Appointment appointment = appointmentMapper.selectById(dto.getAppointmentId());
        if (appointment == null) {
            return Result.fail("预约记录不存在");
        }

        if (!appointment.getDoctorId().toString().equals(doctorId)) {
            return Result.fail("无权呼叫此患者");
        }

        if (appointment.getStatus() != 0) {
            return Result.fail("该预约已处理");
        }

        appointment.setStatus(1);
        appointment.setLocation(dto.getLocation());
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        VisitRecord visitRecord = new VisitRecord();
        visitRecord.setAppointmentId(dto.getAppointmentId());
        visitRecord.setPatientId(appointment.getPatientId());
        visitRecord.setDoctorId(appointment.getDoctorId());
        visitRecord.setDepartmentId(appointment.getDepartmentId());
        visitRecord.setVisitNumber(UUID.randomUUID().toString().substring(0, 8));
        visitRecord.setCallStatus(1);
        visitRecord.setCallTime(LocalDateTime.now());
        visitRecord.setCreateTime(LocalDateTime.now());
        visitRecord.setUpdateTime(LocalDateTime.now());
        visitRecordMapper.insert(visitRecord);

        return Result.ok("呼叫成功");
    }

    @Override
    @Transactional
    public Result startVisit(Long appointmentId, HttpSession session) {
        String doctorId = (String) session.getAttribute("account");
        String role = (String) session.getAttribute("role");
        
        if (!"doctor".equals(role)) {
            return Result.fail("无权执行此操作");
        }

        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            return Result.fail("预约记录不存在");
        }

        if (!appointment.getDoctorId().toString().equals(doctorId)) {
            return Result.fail("无权操作此预约");
        }

        appointment.setStatus(2);
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        QueryWrapper<VisitRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("appointment_id", appointmentId);
        VisitRecord visitRecord = visitRecordMapper.selectOne(wrapper);
        if (visitRecord != null) {
            visitRecord.setCheckInTime(LocalDateTime.now());
            visitRecord.setVisitStartTime(LocalDateTime.now());
            visitRecord.setUpdateTime(LocalDateTime.now());
            visitRecordMapper.updateById(visitRecord);
        }

        return Result.ok("开始就诊");
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CACHE_BILLING, allEntries = true)
    public Result endVisit(Long appointmentId, VisitRecordDto dto, HttpSession session) {
        String doctorId = (String) session.getAttribute("account");
        String role = (String) session.getAttribute("role");
        
        if (!"doctor".equals(role)) {
            return Result.fail("无权执行此操作");
        }

        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            return Result.fail("预约记录不存在");
        }

        if (!appointment.getDoctorId().toString().equals(doctorId)) {
            return Result.fail("无权操作此预约");
        }

        appointment.setStatus(3);
        appointment.setUpdateTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        QueryWrapper<VisitRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("appointment_id", appointmentId);
        VisitRecord visitRecord = visitRecordMapper.selectOne(wrapper);
        if (visitRecord != null) {
            visitRecord.setVisitEndTime(LocalDateTime.now());
            visitRecord.setDiagnosis(dto.getDiagnosis());
            visitRecord.setTreatment(dto.getTreatment());
            visitRecord.setNotes(dto.getNotes());
            visitRecord.setUpdateTime(LocalDateTime.now());
            visitRecordMapper.updateById(visitRecord);
        }

        MedicalRecord medicalRecord = new MedicalRecord();
        medicalRecord.setPatientId(appointment.getPatientId());
        medicalRecord.setDoctorId(appointment.getDoctorId());
        medicalRecord.setDepartmentId(appointment.getDepartmentId());
        medicalRecord.setChiefComplaint(dto.getChiefComplaint());
        medicalRecord.setPresentIllness(dto.getPresentIllness());
        medicalRecord.setDiagnosis(dto.getDiagnosis());
        medicalRecord.setTreatmentPlan(dto.getTreatment());
        medicalRecord.setVisitDate(LocalDateTime.now());
        medicalRecord.setStatus(1);
        medicalRecord.setCreateTime(LocalDateTime.now());
        medicalRecord.setUpdateTime(LocalDateTime.now());
        medicalRecordMapper.insert(medicalRecord);

        if (dto.getPrescriptionItems() != null && !dto.getPrescriptionItems().isEmpty()) {
            createPrescriptionAndBilling(appointment, medicalRecord, dto.getPrescriptionItems());
        }

        return Result.ok(medicalRecord);
    }

    private void createPrescriptionAndBilling(Appointment appointment,
                                              MedicalRecord medicalRecord,
                                              List<PrescriptionItemDto> itemDtos) {
        Prescription prescription = new Prescription();
        prescription.setMedicalRecordId(medicalRecord.getId());
        prescription.setPatientId(appointment.getPatientId());
        prescription.setDoctorId(appointment.getDoctorId());
        prescription.setPrescriptionDate(LocalDateTime.now());
        prescription.setStatus(0);
        prescription.setCreateTime(LocalDateTime.now());
        prescription.setUpdateTime(LocalDateTime.now());
        prescriptionMapper.insert(prescription);

        BigDecimal totalAmount = BigDecimal.ZERO;
        StringBuilder itemNames = new StringBuilder();
        for (PrescriptionItemDto dto : itemDtos) {
            Medicine medicine = medicineMapper.selectById(dto.getMedicineId());
            if (medicine == null || medicine.getStatus() == null || medicine.getStatus() != 1) {
                throw new IllegalArgumentException("药品不存在或已停售: " + dto.getMedicineId());
            }

            PrescriptionItem item = new PrescriptionItem();
            item.setPrescriptionId(prescription.getId());
            item.setMedicineId(dto.getMedicineId());
            item.setDosage(dto.getDosage());
            item.setQuantity(dto.getQuantity());
            item.setDays(dto.getDays());
            item.setRemark(dto.getRemark());
            prescriptionItemMapper.insert(item);

            BigDecimal price = medicine.getPrice() == null ? BigDecimal.ZERO : medicine.getPrice();
            totalAmount = totalAmount.add(price.multiply(BigDecimal.valueOf(dto.getQuantity())));
            if (itemNames.length() > 0) {
                itemNames.append("、");
            }
            itemNames.append(medicine.getName());
        }

        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            Billing billing = new Billing();
            billing.setPatientId(appointment.getPatientId());
            billing.setAppointmentId(appointment.getId());
            billing.setMedicalRecordId(medicalRecord.getId());
            billing.setItemType("MEDICINE");
            billing.setItemName(itemNames.length() > 0 ? itemNames.toString() : "处方药品");
            billing.setAmount(totalAmount);
            billing.setDescription("医生开具处方后自动生成的药品费用，处方号：" + prescription.getId());
            billing.setStatus(0);
            billing.setCreateTime(LocalDateTime.now());
            billing.setUpdateTime(LocalDateTime.now());
            billingMapper.insert(billing);
        }

        Billing dispenseFee = new Billing();
        dispenseFee.setPatientId(appointment.getPatientId());
        dispenseFee.setAppointmentId(appointment.getId());
        dispenseFee.setMedicalRecordId(medicalRecord.getId());
        dispenseFee.setItemType("OTHER");
        dispenseFee.setItemName("取药服务费");
        dispenseFee.setAmount(DISPENSE_SERVICE_FEE);
        dispenseFee.setDescription("病历生成处方后自动生成的取药服务费，处方号：" + prescription.getId());
        dispenseFee.setStatus(0);
        dispenseFee.setCreateTime(LocalDateTime.now());
        dispenseFee.setUpdateTime(LocalDateTime.now());
        billingMapper.insert(dispenseFee);
    }

    @Override
    public Result getPatientMedicalRecords(String patientId, HttpSession session) {
        String role = (String) session.getAttribute("role");
        
        if (!"doctor".equals(role) && !"admin".equals(role)) {
            return Result.fail("无权查看患者病历");
        }

        QueryWrapper<MedicalRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("patient_id", patientId)
               .orderByDesc("visit_date");

        List<MedicalRecord> records = medicalRecordMapper.selectList(wrapper);
        return Result.ok(records);
    }
}
