package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.BillingExplanation;
import com.hospitalinfo.hospitalinformationsystem.dto.BillingQueryDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;

/**
 * 医疗费用查询服务接口
 */
public interface IBillingService {

    /** 分页查询费用列表 */
    Result listBillings(String patientId, BillingQueryDto queryDto);

    /** 获取费用详情 */
    Result getBillingDetail(Long billingId);

    /** AI费用解释 */
    Result aiExplainBilling(String patientId, String question, String startDate, String endDate);
}
