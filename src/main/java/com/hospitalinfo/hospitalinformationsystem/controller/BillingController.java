package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.service.IAsyncTaskService;
import com.hospitalinfo.hospitalinformationsystem.service.IBillingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 医疗费用查询控制器（含AI解释）
 */
@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingController {

    private final IBillingService billingService;
    private final IAsyncTaskService asyncTaskService;

    /**
     * 查询当前用户的费用列表
     * GET /billing/list
     */
    @GetMapping("/list")
    public Result listMyBillings(BillingQueryDto queryDto, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        return billingService.listBillings(patientId, queryDto);
    }

    /**
     * 查询指定患者的费用列表（管理员用）
     * GET /billing/{patientId}/list
     */
    @GetMapping("/{patientId}/list")
    public Result listPatientBillings(@PathVariable String patientId, BillingQueryDto queryDto, HttpSession session) {
        // 权限验证：只有管理员/医生/药师可查看其他患者费用
        Object role = session.getAttribute("role");
        String currentAccount = (String) session.getAttribute("account");
        if (!patientId.equals(currentAccount) &&
                (role == null || (!"admin".equals(role) && !"doctor".equals(role) && !"pharmacist".equals(role)))) {
            return Result.fail("无权查看其他患者的费用信息");
        }
        return billingService.listBillings(patientId, queryDto);
    }

    /**
     * 获取费用详情
     * GET /billing/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public Result getBillingDetail(@PathVariable Long id, HttpSession session) {
        String currentPatientId = (String) session.getAttribute("account");
        Object role = session.getAttribute("role");
        return billingService.getBillingDetail(id, currentPatientId, role);
    }

    @PutMapping("/{id}/pay")
    public Result payBilling(@PathVariable Long id,
                             @RequestBody(required = false) BillingPayDto dto,
                             HttpSession session) {
        String currentPatientId = (String) session.getAttribute("account");
        Object role = session.getAttribute("role");
        String paymentMethod = dto == null ? null : dto.getPaymentMethod();
        return billingService.payBilling(id, currentPatientId, role, paymentMethod);
    }

    @PutMapping("/pay-all")
    public Result payAllUnpaid(@RequestBody(required = false) BillingPayDto dto,
                               HttpSession session) {
        String currentPatientId = (String) session.getAttribute("account");
        String paymentMethod = dto == null ? null : dto.getPaymentMethod();
        return billingService.payAllUnpaid(currentPatientId, paymentMethod);
    }

    /**
     * AI费用解释
     * POST /billing/ai-explain
     * 请求体: { "question": "为什么这么贵？", "startDate": "2024-01-01", "endDate": "2024-12-31" }
     */
    @PostMapping("/ai-explain")
    public Result aiExplainBilling(@RequestBody Map<String, String> body, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        String question = body.getOrDefault("question", "请解释我的费用构成");
        String startDate = body.get("startDate");
        String endDate = body.get("endDate");
        return billingService.aiExplainBilling(patientId, question, startDate, endDate);
    }

    /**
     * AI多轮对话式费用解释（同步）
     * POST /billing/ai-chat
     * 请求体: { "message": "为什么这么贵？", "startDate": "2024-01-01", "endDate": "2024-12-31", "history": [...] }
     */
    @PostMapping("/ai-chat")
    public Result aiBillingChat(@RequestBody Map<String, Object> body, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        String message = (String) body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Result.fail("请输入您的消息");
        }
        String startDate = (String) body.get("startDate");
        String endDate = (String) body.get("endDate");

        // 解析历史消息
        @SuppressWarnings("unchecked")
        List<Map<String, String>> historyRaw = (List<Map<String, String>>) body.get("history");
        List<ChatMessageDto> history = null;
        if (historyRaw != null) {
            history = historyRaw.stream()
                    .map(m -> new ChatMessageDto(m.get("role"), m.get("content")))
                    .toList();
        }

        return billingService.aiBillingChat(patientId, message, history, startDate, endDate);
    }

    /**
     * AI多轮对话式费用解释（异步，提交任务返回taskId）
     * POST /billing/ai-chat-async
     * 请求体: { "message": "为什么这么贵？", "startDate": "2024-01-01", "endDate": "2024-12-31", "history": [...] }
     */
    @PostMapping("/ai-chat-async")
    public Result aiBillingChatAsync(@RequestBody Map<String, Object> body, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        String message = (String) body.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Result.fail("请输入您的消息");
        }
        String startDate = (String) body.get("startDate");
        String endDate = (String) body.get("endDate");

        // 解析历史消息
        @SuppressWarnings("unchecked")
        List<Map<String, String>> historyRaw = (List<Map<String, String>>) body.get("history");
        List<ChatMessageDto> history = null;
        if (historyRaw != null) {
            history = historyRaw.stream()
                    .map(m -> new ChatMessageDto(m.get("role"), m.get("content")))
                    .toList();
        }

        return billingService.aiBillingChatAsync(patientId, message, history, startDate, endDate);
    }

    /**
     * 查询异步任务结果
     * GET /billing/ai-task/{taskId}
     */
    @GetMapping("/ai-task/{taskId}")
    public Result getAiTaskResult(@PathVariable String taskId, HttpSession session) {
        String currentPatientId = (String) session.getAttribute("account");
        AsyncTaskResult taskResult = asyncTaskService.getTaskResult(taskId);
        // 权限验证：只有任务所属患者才能查看结果
        if (taskResult != null && taskResult.getPatientId() != null
                && !taskResult.getPatientId().equals(currentPatientId)) {
            Object role = session.getAttribute("role");
            if (role == null || (!"admin".equals(role) && !"doctor".equals(role))) {
                return Result.fail("无权查看该任务结果");
            }
        }
        return Result.ok(taskResult);
    }
}
