package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.BillingQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.service.IBillingService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 医疗费用查询控制器（含AI解释）
 */
@RestController
@RequestMapping("/billing")
@RequiredArgsConstructor
public class BillingController {

    private final IBillingService billingService;

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
    public Result listPatientBillings(@PathVariable String patientId, BillingQueryDto queryDto) {
        return billingService.listBillings(patientId, queryDto);
    }

    /**
     * 获取费用详情
     * GET /billing/detail/{id}
     */
    @GetMapping("/detail/{id}")
    public Result getBillingDetail(@PathVariable Long id) {
        return billingService.getBillingDetail(id);
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
}
