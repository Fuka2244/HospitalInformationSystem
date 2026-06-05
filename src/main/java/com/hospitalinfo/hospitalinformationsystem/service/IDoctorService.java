package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.DoctorCallPatientDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.VisitRecordDto;
import jakarta.servlet.http.HttpSession;

public interface IDoctorService {
    Result getTodayAppointments(HttpSession session);
    Result callPatient(DoctorCallPatientDto dto, HttpSession session);
    Result startVisit(Long appointmentId, HttpSession session);
    Result endVisit(Long appointmentId, VisitRecordDto dto, HttpSession session);
    Result getPatientMedicalRecords(String patientId, HttpSession session);
}