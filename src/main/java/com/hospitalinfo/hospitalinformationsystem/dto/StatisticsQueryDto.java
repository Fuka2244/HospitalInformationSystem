package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StatisticsQueryDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long departmentId;
    private Long doctorId;
    private String statisticsType;
}