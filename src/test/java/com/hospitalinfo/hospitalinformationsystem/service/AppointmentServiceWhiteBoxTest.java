package com.hospitalinfo.hospitalinformationsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.ai.AiAppointmentService;
import com.hospitalinfo.hospitalinformationsystem.dto.AppointmentCreateDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.Appointment;
import com.hospitalinfo.hospitalinformationsystem.entity.Department;
import com.hospitalinfo.hospitalinformationsystem.entity.Doctor;
import com.hospitalinfo.hospitalinformationsystem.entity.DoctorSchedule;
import com.hospitalinfo.hospitalinformationsystem.mapper.AppointmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.BillingMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DepartmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorScheduleMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.PatientMapper;
import com.hospitalinfo.hospitalinformationsystem.service.impl.AppointmentServiceImpl;
import com.hospitalinfo.hospitalinformationsystem.utils.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Student B white-box tests - appointment service")
class AppointmentServiceWhiteBoxTest {

    @Mock private AppointmentMapper appointmentMapper;
    @Mock private DoctorScheduleMapper doctorScheduleMapper;
    @Mock private DoctorMapper doctorMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private PatientMapper patientMapper;
    @Mock private BillingMapper billingMapper;
    @Mock private AiAppointmentService aiAppointmentService;
    @Mock private RedisDistributedLock distributedLock;
    @Mock private IScheduleStockService scheduleStockService;

    private AppointmentServiceImpl service;
    private AppointmentCreateDto dto;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        service = new AppointmentServiceImpl(
                appointmentMapper,
                doctorScheduleMapper,
                doctorMapper,
                departmentMapper,
                patientMapper,
                billingMapper,
                aiAppointmentService,
                distributedLock,
                scheduleStockService);

        dto = new AppointmentCreateDto();
        dto.setDepartmentId(10L);
        dto.setDoctorId(20L);
        dto.setAppointmentType("DOCTOR");
        dto.setAppointmentDate(LocalDate.of(2026, 6, 20));
        dto.setTimeSlot("09:00-10:00");

        appointment = new Appointment();
        appointment.setId(100L);
        appointment.setPatientId("patient-b");
        appointment.setDepartmentId(10L);
        appointment.setDoctorId(20L);
        appointment.setAppointmentDate(LocalDate.of(2026, 6, 20));
        appointment.setTimeSlot("09:00-10:00");
        appointment.setStatus(0);
    }

    @Nested
    @DisplayName("createAppointment logic coverage")
    class CreateAppointmentLogicCoverage {

        @Test
        @DisplayName("C1 false: doctor is missing, method exits before lock")
        void createRejectsMissingDoctor() {
            dto.setDoctorId(null);

            Result<?> result = service.createAppointment(dto, "patient-b");

            assertFalse(result.getSuccess());
            verify(distributedLock, never()).tryLock(anyString());
            verify(appointmentMapper, never()).insert(any(Appointment.class));
        }

        @Test
        @DisplayName("C2 false: lock acquisition fails")
        void createRejectsWhenLockBusy() {
            when(distributedLock.tryLock(anyString())).thenReturn(null);

            Result<?> result = service.createAppointment(dto, "patient-b");

            assertFalse(result.getSuccess());
            verify(appointmentMapper, never()).selectCount(any(QueryWrapper.class));
            verify(doctorScheduleMapper, never()).incrementBookedCount(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("C3 true: duplicate active appointment is blocked")
        void createRejectsDuplicateAppointment() {
            when(distributedLock.tryLock(anyString())).thenReturn("lock-token");
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

            Result<?> result = service.createAppointment(dto, "patient-b");

            assertFalse(result.getSuccess());
            verify(doctorScheduleMapper, never()).incrementBookedCount(anyLong(), any(), anyString());
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("C4 false: schedule is full or unavailable")
        void createRejectsFullSchedule() {
            when(distributedLock.tryLock(anyString())).thenReturn("lock-token");
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(doctorScheduleMapper.incrementBookedCount(anyLong(), any(), anyString())).thenReturn(0);

            Result<?> result = service.createAppointment(dto, "patient-b");

            assertFalse(result.getSuccess());
            verify(appointmentMapper, never()).insert(any(Appointment.class));
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("C1/C2/C3/C4 success path inserts an appointment")
        void createSucceedsWhenAllConditionsPass() {
            when(distributedLock.tryLock(anyString())).thenReturn("lock-token");
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(doctorScheduleMapper.incrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);
            when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);

            Result<?> result = service.createAppointment(dto, "patient-b");

            assertTrue(result.getSuccess());
            assertInstanceOf(Appointment.class, result.getData());
            verify(appointmentMapper).insert(any(Appointment.class));
            verify(distributedLock).unlock(anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("cancel and reschedule path coverage")
    class CancelAndReschedulePathCoverage {

        @Test
        @DisplayName("P1: appointment does not exist")
        void cancelRejectsMissingAppointment() {
            when(appointmentMapper.selectById(100L)).thenReturn(null);

            Result<?> result = service.cancelAppointment(100L, "change plan", "patient-b");

            assertFalse(result.getSuccess());
            verify(appointmentMapper, never()).updateById(any(Appointment.class));
        }

        @Test
        @DisplayName("P2: appointment belongs to another patient")
        void cancelRejectsNonOwner() {
            when(appointmentMapper.selectById(100L)).thenReturn(appointment);

            Result<?> result = service.cancelAppointment(100L, "change plan", "other-patient");

            assertFalse(result.getSuccess());
            verify(appointmentMapper, never()).updateById(any(Appointment.class));
        }

        @Test
        @DisplayName("P3: appointment status is not reserved")
        void cancelRejectsWrongStatus() {
            appointment.setStatus(1);
            when(appointmentMapper.selectById(100L)).thenReturn(appointment);

            Result<?> result = service.cancelAppointment(100L, "change plan", "patient-b");

            assertFalse(result.getSuccess());
            verify(doctorScheduleMapper, never()).decrementBookedCount(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("P4: successful cancel releases schedule under lock")
        void cancelSucceedsAndReleasesScheduleWithLock() {
            when(appointmentMapper.selectById(100L)).thenReturn(appointment);
            when(appointmentMapper.updateById(any(Appointment.class))).thenReturn(1);
            when(distributedLock.tryLock(anyString())).thenReturn("lock-token");
            when(doctorScheduleMapper.decrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);

            Result<?> result = service.cancelAppointment(100L, "change plan", "patient-b");

            assertTrue(result.getSuccess());
            assertEquals(2, appointment.getStatus());
            verify(doctorScheduleMapper).decrementBookedCount(20L, LocalDate.of(2026, 6, 20), "09:00-10:00");
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("P5: reschedule stops when cancel fails")
        void rescheduleStopsWhenCancelFails() {
            when(appointmentMapper.selectById(100L)).thenReturn(null);

            Result<?> result = service.rescheduleAppointment(100L, dto, "patient-b");

            assertFalse(result.getSuccess());
            verify(appointmentMapper, never()).insert(any(Appointment.class));
        }

        @Test
        @DisplayName("P6: reschedule cancels old appointment and creates a new one")
        void rescheduleCancelsThenCreatesNewAppointment() {
            when(appointmentMapper.selectById(100L)).thenReturn(appointment);
            when(appointmentMapper.updateById(any(Appointment.class))).thenReturn(1);
            when(distributedLock.tryLock(anyString())).thenReturn("lock-token");
            when(doctorScheduleMapper.decrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(doctorScheduleMapper.incrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);
            when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);

            Result<?> result = service.rescheduleAppointment(100L, dto, "patient-b");

            assertTrue(result.getSuccess());
            verify(appointmentMapper).updateById(any(Appointment.class));
            verify(appointmentMapper).insert(any(Appointment.class));
        }
    }

    @Nested
    @DisplayName("getAvailableSchedules condition and boundary coverage")
    class AvailableScheduleCoverage {

        @Test
        @DisplayName("C1 true: department-only query uses cached schedule branch")
        void departmentOnlyQueryUsesCacheBranch() {
            Doctor doctor = doctor(20L, 10L, "Dr A");
            Department department = department(10L, "Internal");
            DoctorSchedule schedule = schedule(1L, 20L, LocalDate.of(2026, 6, 20), 5, 1);

            when(aiAppointmentService.getDepartmentSchedulesFromCache(10L)).thenReturn(List.of(schedule));
            when(aiAppointmentService.getDoctorsByDepartmentFromCache(10L)).thenReturn(List.of(doctor));
            when(departmentMapper.selectById(10L)).thenReturn(department);

            Result<?> result = service.getAvailableSchedules(10L, null, "");

            assertTrue(result.getSuccess());
            List<?> schedules = (List<?>) result.getData();
            DoctorSchedule copied = (DoctorSchedule) schedules.get(0);
            assertEquals("Dr A", copied.getDoctorName());
            assertEquals("Internal", copied.getDepartmentName());
            verify(doctorScheduleMapper, never()).selectList(any(QueryWrapper.class));
        }

        @Test
        @DisplayName("C2 true: department has no active doctors")
        void directQueryReturnsEmptyWhenDepartmentHasNoDoctors() {
            when(doctorMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

            Result<?> result = service.getAvailableSchedules(999L, 20L, null);

            assertTrue(result.getSuccess());
            assertTrue(((List<?>) result.getData()).isEmpty());
        }

        @Test
        @DisplayName("C3 true: exact date query fills doctor and department names")
        void directDateQueryFillsNames() {
            Doctor doctor = doctor(20L, 10L, "Dr A");
            Department department = department(10L, "Internal");
            DoctorSchedule schedule = schedule(1L, 20L, LocalDate.of(2026, 6, 20), 5, 1);

            when(doctorScheduleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(schedule));
            when(doctorMapper.selectById(20L)).thenReturn(doctor);
            when(departmentMapper.selectById(10L)).thenReturn(department);

            Result<?> result = service.getAvailableSchedules(null, 20L, "2026-06-20");

            assertTrue(result.getSuccess());
            DoctorSchedule filled = (DoctorSchedule) ((List<?>) result.getData()).get(0);
            assertEquals("Dr A", filled.getDoctorName());
            assertEquals("Internal", filled.getDepartmentName());
        }

        @Test
        @DisplayName("boundary/exception path: invalid date is rejected by parser")
        void invalidDateThrowsParseException() {
            assertThrows(DateTimeParseException.class,
                    () -> service.getAvailableSchedules(null, 20L, "2026/06/20"));
        }
    }

    private static Doctor doctor(Long id, Long departmentId, String name) {
        Doctor doctor = new Doctor();
        doctor.setId(id);
        doctor.setDepartmentId(departmentId);
        doctor.setName(name);
        doctor.setTitle("Chief");
        doctor.setSpecialty("General");
        doctor.setStatus(1);
        return doctor;
    }

    private static Department department(Long id, String name) {
        Department department = new Department();
        department.setId(id);
        department.setName(name);
        department.setStatus(1);
        return department;
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
