package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.BillingQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;

import java.util.List;

public interface IBillingService {
    Result listBillings(String patientId, BillingQueryDto queryDto);

    Result getBillingDetail(Long billingId, String currentPatientId, Object role);

    Result payBilling(Long billingId, String currentPatientId, Object role, String paymentMethod);

    Result payAllUnpaid(String patientId, String paymentMethod);

    Result aiExplainBilling(String patientId, String question, String startDate, String endDate);

    Result aiBillingChat(String patientId, String message, List<ChatMessageDto> history, String startDate, String endDate);

    Result aiBillingChatAsync(String patientId, String message, List<ChatMessageDto> history, String startDate, String endDate);
}
