package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiBillingService;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingExplanation;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.Billing;
import com.hospitalinfo.hospitalinformationsystem.entity.Patient;
import com.hospitalinfo.hospitalinformationsystem.mapper.BillingMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.PatientMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IBillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements IBillingService {

    private final BillingMapper billingMapper;
    private final PatientMapper patientMapper;
    private final AiBillingService aiBillingService;

    @Override
    public Result listBillings(String patientId, BillingQueryDto queryDto) {
        Page<Billing> pageParam = new Page<>(queryDto.getPage(), queryDto.getSize());
        QueryWrapper<Billing> wrapper = new QueryWrapper<Billing>()
                .eq("patient_id", patientId);

        if (queryDto.getItemType() != null && !queryDto.getItemType().isEmpty()) {
            wrapper.eq("item_type", queryDto.getItemType());
        }
        if (queryDto.getStatus() != null) {
            wrapper.eq("status", queryDto.getStatus());
        }
        if (queryDto.getStartDate() != null && !queryDto.getStartDate().isEmpty()) {
            wrapper.ge("create_time", queryDto.getStartDate() + " 00:00:00");
        }
        if (queryDto.getEndDate() != null && !queryDto.getEndDate().isEmpty()) {
            wrapper.le("create_time", queryDto.getEndDate() + " 23:59:59");
        }
        wrapper.orderByDesc("create_time");

        Page<Billing> result = billingMapper.selectPage(pageParam, wrapper);

        // 填充患者名称
        for (Billing b : result.getRecords()) {
            Patient patient = patientMapper.selectById(b.getPatientId());
            if (patient != null) b.setPatientName(patient.getName());
        }

        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    public Result getBillingDetail(Long billingId) {
        Billing billing = billingMapper.selectById(billingId);
        if (billing == null) {
            return Result.fail("费用记录不存在");
        }
        Patient patient = patientMapper.selectById(billing.getPatientId());
        if (patient != null) billing.setPatientName(patient.getName());
        return Result.ok(billing);
    }

    @Override
    public Result aiExplainBilling(String patientId, String question, String startDate, String endDate) {
        QueryWrapper<Billing> wrapper = new QueryWrapper<Billing>()
                .eq("patient_id", patientId);

        if (startDate != null && !startDate.isEmpty()) {
            wrapper.ge("create_time", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            wrapper.le("create_time", endDate + " 23:59:59");
        }

        List<Billing> billings = billingMapper.selectList(wrapper);
        if (billings.isEmpty()) {
            return Result.fail("该时间段内无费用记录");
        }

        BillingExplanation explanation = aiBillingService.explainBilling(billings, question);
        return Result.ok(explanation);
    }
}
