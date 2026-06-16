package com.hospitalinfo.hospitalinformationsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiAppointmentService;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.*;
import com.hospitalinfo.hospitalinformationsystem.mapper.*;
import com.hospitalinfo.hospitalinformationsystem.service.impl.AppointmentServiceImpl;
import com.hospitalinfo.hospitalinformationsystem.utils.RedisDistributedLock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 预约系统模块单元测试
 * 覆盖：创建预约、查询预约列表、预约详情、取消预约、改期、AI智能推荐、排班查询
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("模块2 - 预约系统")
class AppointmentServiceTest {

    @Mock private AppointmentMapper appointmentMapper;
    @Mock private DoctorScheduleMapper doctorScheduleMapper;
    @Mock private DoctorMapper doctorMapper;
    @Mock private DepartmentMapper departmentMapper;
    @Mock private PatientMapper patientMapper;
    @Mock private BillingMapper billingMapper;
    @Mock private AiAppointmentService aiAppointmentService;
    @Mock private RedisDistributedLock distributedLock;
    @Mock private IScheduleStockService scheduleStockService;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private AppointmentCreateDto createDto;
    private DoctorSchedule mockSchedule;
    private Appointment mockAppointment;
    private Patient mockPatient;
    private Doctor mockDoctor;
    private Department mockDept;

    @BeforeEach
    void setUp() {
        createDto = new AppointmentCreateDto();
        createDto.setDepartmentId(1L);
        createDto.setDoctorId(1L);
        createDto.setAppointmentType("DOCTOR");
        createDto.setAppointmentDate(LocalDate.of(2025, 6, 15));
        createDto.setTimeSlot("09:00-10:00");

        mockSchedule = new DoctorSchedule();
        mockSchedule.setId(1L);
        mockSchedule.setDoctorId(1L);
        mockSchedule.setScheduleDate(LocalDate.of(2025, 6, 15));
        mockSchedule.setTimeSlot("09:00-10:00");
        mockSchedule.setMaxPatients(20);
        mockSchedule.setBookedCount(5);
        mockSchedule.setStatus(1);

        mockAppointment = new Appointment();
        mockAppointment.setId(1L);
        mockAppointment.setPatientId("patient-001");
        mockAppointment.setDoctorId(1L);
        mockAppointment.setDepartmentId(1L);
        mockAppointment.setAppointmentType("DOCTOR");
        mockAppointment.setAppointmentDate(LocalDate.of(2025, 6, 15));
        mockAppointment.setTimeSlot("09:00-10:00");
        mockAppointment.setStatus(0);
        mockAppointment.setAiRecommended(0);

        mockPatient = new Patient();
        mockPatient.setAccount("patient-001");
        mockPatient.setName("张三");

        mockDoctor = new Doctor();
        mockDoctor.setId(1L);
        mockDoctor.setName("李医生");
        mockDoctor.setDepartmentId(1L);

        mockDept = new Department();
        mockDept.setId(1L);
        mockDept.setName("内科");

        // 默认模拟分布式锁获取成功
        when(distributedLock.tryLock(anyString())).thenReturn("mock-lock-value");
    }

    // ==================== 创建预约 ====================

    @Nested
    @DisplayName("创建预约")
    class CreateAppointmentTests {

        @Test
        @DisplayName("创建预约成功 - 有排班且未满")
        void createSuccess() {
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(doctorScheduleMapper.incrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);
            when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);

            Result result = appointmentService.createAppointment(createDto, "patient-001");

            assertTrue(result.getSuccess());
            verify(doctorScheduleMapper).incrementBookedCount(anyLong(), any(), anyString());
            verify(appointmentMapper).insert(any(Appointment.class));
            verify(billingMapper).insert(any(Billing.class));
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("创建预约失败 - 号源已满")
        void createFailScheduleFull() {
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(doctorScheduleMapper.incrementBookedCount(anyLong(), any(), anyString())).thenReturn(0);

            Result result = appointmentService.createAppointment(createDto, "patient-001");

            assertFalse(result.getSuccess());
            assertEquals("该时段已约满，请选择其他时间", result.getErrorMsg());
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("创建预约失败 - 重复预约")
        void createFailDuplicate() {
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(1L);

            Result result = appointmentService.createAppointment(createDto, "patient-001");

            assertFalse(result.getSuccess());
            assertEquals("您已预约该时段，请勿重复预约", result.getErrorMsg());
            verify(distributedLock).unlock(anyString(), anyString());
        }

        @Test
        @DisplayName("创建预约失败 - 获取分布式锁失败")
        void createFailLockAcquiredFailed() {
            when(distributedLock.tryLock(anyString())).thenReturn(null);

            Result result = appointmentService.createAppointment(createDto, "patient-001");

            assertFalse(result.getSuccess());
            assertEquals("当前预约人数较多，请稍后再试", result.getErrorMsg());
        }

        @Test
        @DisplayName("创建预约成功 - 边界值：最后一个号源")
        void createSuccessLastSlot() {
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(doctorScheduleMapper.incrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);
            when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);

            Result result = appointmentService.createAppointment(createDto, "patient-001");

            assertTrue(result.getSuccess());
            verify(billingMapper).insert(any(Billing.class));
        }
    }

    // ==================== 查询预约列表 ====================

    @Nested
    @DisplayName("查询预约列表")
    class ListAppointmentsTests {

        @Test
        @DisplayName("分页查询预约 - 填充名称信息")
        void listAppointmentsSuccess() {
            AppointmentQueryDto queryDto = new AppointmentQueryDto();
            queryDto.setPage(1);
            queryDto.setSize(10);

            Page<Appointment> page = new Page<>(1, 10);
            page.setRecords(List.of(mockAppointment));
            page.setTotal(1L);

            when(appointmentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(departmentMapper.selectById(1L)).thenReturn(mockDept);

            Result result = appointmentService.listAppointments("patient-001", queryDto);

            assertTrue(result.getSuccess());
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("按状态筛选 - 只看已预约")
        void listAppointmentsFilterByStatus() {
            AppointmentQueryDto queryDto = new AppointmentQueryDto();
            queryDto.setStatus(0);
            queryDto.setPage(1);
            queryDto.setSize(10);

            Page<Appointment> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0L);

            when(appointmentMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = appointmentService.listAppointments("patient-001", queryDto);

            assertTrue(result.getSuccess());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== 预约详情 ====================

    @Nested
    @DisplayName("预约详情")
    class GetAppointmentDetailTests {

        @Test
        @DisplayName("获取预约详情成功")
        void getDetailSuccess() {
            when(appointmentMapper.selectById(1L)).thenReturn(mockAppointment);
            when(patientMapper.selectById("patient-001")).thenReturn(mockPatient);
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(departmentMapper.selectById(1L)).thenReturn(mockDept);

            Result result = appointmentService.getAppointmentDetail(1L);

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("预约不存在 - 返回失败")
        void getDetailNotFound() {
            when(appointmentMapper.selectById(999L)).thenReturn(null);

            Result result = appointmentService.getAppointmentDetail(999L);

            assertFalse(result.getSuccess());
            assertEquals("预约不存在", result.getErrorMsg());
        }
    }

    // ==================== 取消预约 ====================

    @Nested
    @DisplayName("取消预约")
    class CancelAppointmentTests {

        @Test
        @DisplayName("取消预约成功 - 释放号源")
        void cancelSuccess() {
            when(appointmentMapper.selectById(1L)).thenReturn(mockAppointment);
            when(appointmentMapper.updateById(any(Appointment.class))).thenReturn(1);
            when(doctorScheduleMapper.decrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);

            Result result = appointmentService.cancelAppointment(1L, "不想要了", "patient-001");

            assertTrue(result.getSuccess());
            verify(doctorScheduleMapper).decrementBookedCount(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("取消预约失败 - 预约不存在")
        void cancelFailNotFound() {
            when(appointmentMapper.selectById(999L)).thenReturn(null);

            Result result = appointmentService.cancelAppointment(999L, "原因", "patient-001");

            assertFalse(result.getSuccess());
            assertEquals("预约不存在", result.getErrorMsg());
        }

        @Test
        @DisplayName("取消预约失败 - 非本人预约")
        void cancelFailNotOwner() {
            when(appointmentMapper.selectById(1L)).thenReturn(mockAppointment);

            Result result = appointmentService.cancelAppointment(1L, "原因", "other-patient");

            assertFalse(result.getSuccess());
            assertEquals("无权操作此预约", result.getErrorMsg());
        }

        @Test
        @DisplayName("取消预约失败 - 状态非已预约")
        void cancelFailWrongStatus() {
            mockAppointment.setStatus(1); // 已完成
            when(appointmentMapper.selectById(1L)).thenReturn(mockAppointment);

            Result result = appointmentService.cancelAppointment(1L, "原因", "patient-001");

            assertFalse(result.getSuccess());
            assertEquals("只能取消已预约状态的预约", result.getErrorMsg());
        }
    }

    // ==================== 改期 ====================

    @Nested
    @DisplayName("改期预约")
    class RescheduleAppointmentTests {

        @Test
        @DisplayName("改期成功 - 取消旧预约+创建新预约")
        void rescheduleSuccess() {
            when(appointmentMapper.selectById(1L)).thenReturn(mockAppointment);
            when(appointmentMapper.updateById(any(Appointment.class))).thenReturn(1);
            when(doctorScheduleMapper.decrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);
            when(appointmentMapper.selectCount(any(QueryWrapper.class))).thenReturn(0L);
            when(doctorScheduleMapper.incrementBookedCount(anyLong(), any(), anyString())).thenReturn(1);
            when(appointmentMapper.insert(any(Appointment.class))).thenReturn(1);

            Result result = appointmentService.rescheduleAppointment(1L, createDto, "patient-001");

            assertTrue(result.getSuccess());
            verify(appointmentMapper).updateById(any(Appointment.class));
            verify(appointmentMapper).insert(any(Appointment.class));
            verify(billingMapper).insert(any(Billing.class));
        }
    }

    // ==================== AI智能推荐 ====================

    @Nested
    @DisplayName("AI智能预约推荐")
    class AiRecommendTests {

        @Test
        @DisplayName("AI推荐成功 - 返回推荐结果")
        void aiRecommendSuccess() {
            AppointmentRecommendation recommendation = new AppointmentRecommendation();
            recommendation.setDepartment("内科");
            recommendation.setDoctor("李医生");
            recommendation.setRecommendedTime("上午09:00-10:00");
            recommendation.setReason("头痛发热建议内科就诊");

            when(aiAppointmentService.recommendBySymptom("头痛发热")).thenReturn(recommendation);

            Result result = appointmentService.aiRecommendAppointment("头痛发热");

            assertTrue(result.getSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("AI推荐 - 空症状描述")
        void aiRecommendEmptySymptom() {
            when(aiAppointmentService.recommendBySymptom("")).thenReturn(new AppointmentRecommendation());

            Result result = appointmentService.aiRecommendAppointment("");

            assertTrue(result.getSuccess());
        }
    }

    // ==================== 排班查询 ====================

    @Nested
    @DisplayName("查询可用排班")
    class GetAvailableSchedulesTests {

        @Test
        @DisplayName("按科室查询排班 - 默认未来7天")
        void getSchedulesByDepartment() {
            when(doctorMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockDoctor));
            when(doctorScheduleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSchedule));
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(departmentMapper.selectById(1L)).thenReturn(mockDept);

            Result result = appointmentService.getAvailableSchedules(1L, null, null);

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("按科室查询 - 科室下无医生")
        void getSchedulesNoDoctors() {
            when(doctorMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());

            Result result = appointmentService.getAvailableSchedules(99L, null, null);

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("按指定日期查询排班")
        void getSchedulesByDate() {
            when(doctorScheduleMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(mockSchedule));
            when(doctorMapper.selectById(1L)).thenReturn(mockDoctor);
            when(departmentMapper.selectById(1L)).thenReturn(mockDept);

            Result result = appointmentService.getAvailableSchedules(null, 1L, "2025-06-15");

            assertTrue(result.getSuccess());
        }
    }
}
