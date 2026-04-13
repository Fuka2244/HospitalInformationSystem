package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("medicine")
public class Medicine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    @TableField("generic_name")
    private String genericName;
    private String category;
    private String specification;
    private String manufacturer;
    private String ingredients;
    private String efficacy;
    @TableField("side_effects")
    private String sideEffects;
    private String contraindications;
    private BigDecimal price;
    private Integer stock;
    private Integer status;
    @TableField("create_time")
    private LocalDateTime createTime;
    @TableField("update_time")
    private LocalDateTime updateTime;
}
