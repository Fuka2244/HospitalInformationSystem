package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("doctor_schedule")
public class DoctorSchedule {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("doctor_id")
    private Long doctorId;
    @TableField("schedule_date")
    private LocalDate scheduleDate;
    @TableField("time_slot")
    private String timeSlot;
    @TableField("max_patients")
    private Integer maxPatients;
    @TableField("booked_count")
    private Integer bookedCount;
    private Integer status;

    /** 非数据库字段 */
    @TableField(exist = false)
    private Long departmentId;
    @TableField(exist = false)
    private String doctorName;
    @TableField(exist = false)
    private String departmentName;
}
