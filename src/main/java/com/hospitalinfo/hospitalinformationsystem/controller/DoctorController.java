package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.DoctorCallPatientDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.VisitRecordDto;
import com.hospitalinfo.hospitalinformationsystem.service.IDoctorService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/doctor")
@RequiredArgsConstructor
public class DoctorController {

    private final IDoctorService doctorService;

    @GetMapping("/today-appointments")
    public Result getTodayAppointments(HttpSession session) {
        return doctorService.getTodayAppointments(session);
    }

    @PostMapping("/call-patient")
    public Result callPatient(@RequestBody @Valid DoctorCallPatientDto dto, HttpSession session) {
        return doctorService.callPatient(dto, session);
    }

    @PutMapping("/start-visit/{appointmentId}")
    public Result startVisit(@PathVariable Long appointmentId, HttpSession session) {
        return doctorService.startVisit(appointmentId, session);
    }

    @PutMapping("/end-visit/{appointmentId}")
    public Result endVisit(@PathVariable Long appointmentId, @RequestBody VisitRecordDto dto, HttpSession session) {
        return doctorService.endVisit(appointmentId, dto, session);
    }

    @GetMapping("/patient/{patientId}/medical-records")
    public Result getPatientMedicalRecords(@PathVariable String patientId, HttpSession session) {
        return doctorService.getPatientMedicalRecords(patientId, session);
    }
}