package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 费用查询请求参数
 */
@Data
public class BillingQueryDto {
    private String itemType;     // REGISTRATION/EXAMINATION/MEDICINE/OTHER
    private String startDate;
    private String endDate;
    private Integer status;      // 0未支付/1已支付/2已退款
    private Integer page = 1;
    private Integer size = 10;
}
