package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.BillingExplanation;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;

import java.util.List;

/**
 * 医疗费用查询服务接口
 */
public interface IBillingService {

    /** 分页查询费用列表 */
    Result listBillings(String patientId, BillingQueryDto queryDto);

    /** 获取费用详情（含权限验证） */
    Result getBillingDetail(Long billingId, String currentPatientId, Object role);

    /** AI费用解释 */
    Result aiExplainBilling(String patientId, String question, String startDate, String endDate);

    /** AI多轮对话式费用解释 */
    Result aiBillingChat(String patientId, String message, List<ChatMessageDto> history, String startDate, String endDate);

    /** AI多轮对话式费用解释（异步，返回taskId） */
    Result aiBillingChatAsync(String patientId, String message, List<ChatMessageDto> history, String startDate, String endDate);
}
