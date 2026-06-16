package com.hospitalinfo.hospitalinformationsystem.ai;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.AppointmentRecommendation;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.TriageChatResponse;
import com.hospitalinfo.hospitalinformationsystem.entity.Department;
import com.hospitalinfo.hospitalinformationsystem.entity.Doctor;
import com.hospitalinfo.hospitalinformationsystem.entity.DoctorSchedule;
import com.hospitalinfo.hospitalinformationsystem.mapper.DepartmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorScheduleMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Student B white-box tests - AI appointment service")
class AiAppointmentServiceWhiteBoxTest {

    @Mock private ChatLanguageModel chatModel;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private DoctorMapper doctorMapper;
    @Mock private DoctorScheduleMapper doctorScheduleMapper;

    private AiAppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AiAppointmentService(
                chatModel,
                departmentMapper,
                doctorMapper,
                doctorScheduleMapper,
                new ObjectMapper());
    }

    @Nested
    @DisplayName("recommendWithSchedules path coverage")
    class RecommendWithSchedulesCoverage {

        @Test
        @DisplayName("P1: AI department match with one available doctor selects earliest slot")
        void oneAvailableDoctorIsSelectedAutomatically() {
            Department internal = department(1L, "Internal");
            Doctor doctor = doctor(11L, 1L, "Dr One");
            DoctorSchedule sourceSchedule = schedule(101L, 11L, LocalDate.of(2026, 6, 20), 5, 1);

            when(departmentMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(internal));
            when(chatModel.generate(anyString())).thenReturn("{\"department\":\"Internal\",\"reason\":\"fever\"}");
            when(doctorMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(doctor));
            when(doctorScheduleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(sourceSchedule));

            AppointmentRecommendation result = service.recommendWithSchedules("fever");

            assertEquals("Internal", result.getDepartment());
            assertEquals(11L, result.getDoctorId());
            assertFalse(result.isNeedChooseDoctor());
            assertEquals("2026-06-20", result.getRecommendedDate());
            assertNotSame(sourceSchedule, result.getAvailableDoctors().get(0).getSchedules().get(0));
        }

        @Test
        @DisplayName("P2: multiple doctors require patient choice")
        void multipleDoctorsRequireChoice() {
            Department internal = department(1L, "Internal");
            Doctor first = doctor(11L, 1L, "Dr One");
            Doctor second = doctor(12L, 1L, "Dr Two");

            when(departmentMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(internal));
            when(chatModel.generate(anyString())).thenReturn("{\"department\":\"Internal\",\"reason\":\"cough\"}");
            when(doctorMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(first, second));
            when(doctorScheduleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(
                    schedule(101L, 11L, LocalDate.of(2026, 6, 20), 5, 1),
                    schedule(102L, 12L, LocalDate.of(2026, 6, 21), 5, 0)));

            AppointmentRecommendation result = service.recommendWithSchedules("cough");

            assertEquals(2, result.getAvailableDoctors().size());
            assertTrue(result.isNeedChooseDoctor());
            assertNull(result.getDoctorId());
        }

        @Test
        @DisplayName("P3: matched department has no remaining schedule")
        void noAvailableScheduleReturnsDepartmentWithoutDoctorChoice() {
            Department internal = department(1L, "Internal");

            when(departmentMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(internal));
            when(chatModel.generate(anyString())).thenReturn("{\"department\":\"Internal\",\"reason\":\"review\"}");
            when(doctorMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(doctor(11L, 1L, "Dr One")));
            when(doctorScheduleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

            AppointmentRecommendation result = service.recommendWithSchedules("review");

            assertEquals("Internal", result.getDepartment());
            assertTrue(result.getAvailableDoctors().isEmpty());
            assertFalse(result.isNeedChooseDoctor());
        }

        @Test
        @DisplayName("P4: AI returns invalid JSON and service falls back")
        void invalidAiJsonReturnsFallbackRecommendation() {
            when(departmentMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(department(1L, "Internal")));
            when(chatModel.generate(anyString())).thenReturn("not json");

            AppointmentRecommendation result = service.recommendWithSchedules("unknown");

            assertEquals("全科", result.getDepartment());
            assertNull(result.getDepartmentId());
        }

        @Test
        @DisplayName("P5: AI department cannot be matched and service falls back")
        void unmatchedDepartmentReturnsFallbackRecommendation() {
            when(departmentMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(department(1L, "Internal")));
            when(chatModel.generate(anyString())).thenReturn("{\"department\":\"Surgery\",\"reason\":\"pain\"}");

            AppointmentRecommendation result = service.recommendWithSchedules("pain");

            assertEquals("全科", result.getDepartment());
            assertNull(result.getDepartmentId());
        }
    }

    @Nested
    @DisplayName("triageChat condition coverage")
    class TriageChatCoverage {

        @Test
        @DisplayName("C1 false: conversation is still collecting information")
        void triageOngoingWhenCompletedFalse() {
            when(departmentMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(department(1L, "Internal")));
            when(chatModel.generate(anyString())).thenReturn("""
                    Please describe how long the symptom has lasted.
                    ```json
                    {"completed": false}
                    ```
                    """);

            TriageChatResponse result = service.triageChat("I have a headache", List.of());

            assertFalse(result.isCompleted());
            assertTrue(result.getReply().contains("Please describe"));
            assertNull(result.getRecommendation());
        }

        @Test
        @DisplayName("C1 true: completed conversation builds recommendation from department")
        void triageCompletedBuildsRecommendation() {
            Department internal = department(1L, "Internal");
            Doctor doctor = doctor(11L, 1L, "Dr One");

            when(departmentMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(internal));
            when(chatModel.generate(anyString())).thenReturn("""
                    I recommend Internal.
                    ```json
                    {"completed": true, "department": "Internal", "reason": "fever"}
                    ```
                    """);
            when(doctorMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(doctor));
            when(doctorScheduleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(
                    schedule(101L, 11L, LocalDate.of(2026, 6, 20), 5, 1)));

            TriageChatResponse result = service.triageChat(
                    "fever tomorrow morning",
                    List.of(new ChatMessageDto("user", "fever")));

            assertTrue(result.isCompleted());
            assertEquals("Internal", result.getRecommendation().getDepartment());
            assertFalse(result.getReply().contains("completed"));
        }
    }

    private static Department department(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setDescription(name + " department");
        department.setStatus(1);
        return department;
    }

    private static Doctor doctor(Long id, Long departmentId, String name) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setDepartmentId(departmentId);
        doctor.setName(name);
        doctor.setTitle("Attending");
        doctor.setSpecialty("General");
        doctor.setStatus(1);
        return doctor;
    }

    private static DoctorSchedule schedule(Long id, Long doctorId, LocalDate date, int max, int booked) {
        DoctorSchedule schedule = new DoctorSchedule();
        schedule.setId(id);
        schedule.setDoctorId(doctorId);
        schedule.setScheduleDate(date);
        schedule.setTimeSlot("09:00-10:00");
        schedule.setMaxPatients(max);
        schedule.setBookedCount(booked);
        schedule.setStatus(1);
        return schedule;
    }
}
