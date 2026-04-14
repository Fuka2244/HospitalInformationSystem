package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * 预约查询请求参数
 */
@Data
public class AppointmentQueryDto {
    private Integer status;        // 0已预约/1已完成/2已取消
    private Long departmentId;
    private Long doctorId;
    private String startDate;
    private String endDate;
    private Integer page = 1;
    private Integer size = 10;
}
