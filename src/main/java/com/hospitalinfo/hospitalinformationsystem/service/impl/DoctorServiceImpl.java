package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.dto.DoctorCallPatientDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.VisitRecordDto;
import com.hospitalinfo.hospitalinformationsystem.entity.Appointment;
import com.hospitalinfo.hospitalinformationsystem.entity.MedicalRecord;
import com.hospitalinfo.hospitalinformationsystem.entity.VisitRecord;
import com.hospitalinfo.hospitalinformationsystem.mapper.AppointmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicalRecordMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.VisitRecordMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IDoctorService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements IDoctorService {

    private final AppointmentMapper appointmentMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final VisitRecordMapper visitRecordMapper;

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

        return Result.ok("就诊完成");
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