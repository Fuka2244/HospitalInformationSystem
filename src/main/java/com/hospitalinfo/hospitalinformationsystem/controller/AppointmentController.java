package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.service.IAppointmentService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约系统控制器（含AI智能预约）
 */
@RestController
@RequestMapping("/appointment")
@RequiredArgsConstructor
public class AppointmentController {

    private final IAppointmentService appointmentService;

    /**
     * 创建预约
     * POST /appointment
     */
    @PostMapping
    public Result createAppointment(@RequestBody @Valid AppointmentCreateDto dto,
                                     HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return appointmentService.createAppointment(dto, patientId);
    }

    /**
     * 查询当前用户的预约列表
     * GET /appointment/list
     */
    @GetMapping("/list")
    public Result listMyAppointments(AppointmentQueryDto queryDto, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return appointmentService.listAppointments(patientId, queryDto);
    }

    /**
     * 获取预约详情
     * GET /appointment/{id}
     */
    @GetMapping("/{id}")
    public Result getAppointmentDetail(@PathVariable Long id) {
        return appointmentService.getAppointmentDetail(id);
    }

    /**
     * 取消预约
     * PUT /appointment/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    public Result cancelAppointment(@PathVariable Long id,
                                     @RequestParam(required = false) String cancelReason,
                                     HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return appointmentService.cancelAppointment(id, cancelReason, patientId);
    }

    /**
     * 改期
     * PUT /appointment/{id}/reschedule
     */
    @PutMapping("/{id}/reschedule")
    public Result rescheduleAppointment(@PathVariable Long id,
                                         @RequestBody @Valid AppointmentCreateDto dto,
                                         HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return appointmentService.rescheduleAppointment(id, dto, patientId);
    }

    /**
     * AI智能预约推荐
     * POST /appointment/ai-recommend
     * 请求体: { "symptom": "我最近头痛，应该挂什么科？" }
     */
    @PostMapping("/ai-recommend")
    public Result aiRecommend(@RequestBody java.util.Map<String, String> body) {
        String symptom = body.get("symptom");
        if (symptom == null || symptom.isEmpty()) {
            return Result.fail("请描述您的症状");
        }
        return appointmentService.aiRecommendAppointment(symptom);
    }

    /**
     * AI智能预约推荐并查询可用排班
     * POST /appointment/ai-recommend-with-schedules
     * 请求体: { "symptom": "我最近头痛，应该挂什么科？" }
     */
    @PostMapping("/ai-recommend-with-schedules")
    public Result aiRecommendWithSchedules(@RequestBody java.util.Map<String, String> body) {
        String symptom = body.get("symptom");
        if (symptom == null || symptom.isEmpty()) {
            return Result.fail("请描述您的症状");
        }
        return appointmentService.aiRecommendWithSchedules(symptom);
    }

    /**
     * AI多轮对话式智能导诊
     * POST /appointment/ai-triage-chat
     * 请求体: { "message": "我头痛", "history": [{"role":"user","content":"..."},{"role":"assistant","content":"..."}] }
     */
    @PostMapping("/ai-triage-chat")
    public Result aiTriageChat(@RequestBody TriageChatRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Result.fail("请输入您的消息");
        }
        return appointmentService.aiTriageChat(request.getMessage(), request.getHistory());
    }

    /**
     * 获取可用排班
     * GET /appointment/schedules?departmentId=1&doctorId=1&date=2024-01-15
     */
    @GetMapping("/schedules")
    public Result getAvailableSchedules(@RequestParam(required = false) Long departmentId,
                                         @RequestParam(required = false) Long doctorId,
                                         @RequestParam(required = false) String date) {
        return appointmentService.getAvailableSchedules(departmentId, doctorId, date);
    }

    /**
     * 挂号登记
     * POST /appointment/{id}/registration
     */
    @PostMapping("/{id}/registration")
    public Result registration(@PathVariable Long id, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return appointmentService.registration(id, patientId);
    }

    /**
     * 前台登记/现场挂号
     * POST /appointment/frontdesk/registration
     */
    @PostMapping("/frontdesk/registration")
    public Result frontDeskRegistration(@RequestBody @Valid FrontDeskRegistrationDto dto, HttpSession session) {
        Object role = session.getAttribute("role");
        if (role == null || (!"admin".equals(role) && !"doctor".equals(role))) {
            return Result.fail("无权执行前台登记");
        }
        return appointmentService.frontDeskRegistration(dto);
    }

    /**
     * 获取就诊地点
     * GET /appointment/{id}/location
     */
    @GetMapping("/{id}/location")
    public Result getLocation(@PathVariable Long id, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return appointmentService.getLocation(id, patientId);
    }
}
