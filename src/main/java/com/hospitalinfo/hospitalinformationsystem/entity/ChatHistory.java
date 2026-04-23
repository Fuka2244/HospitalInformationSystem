package com.hospitalinfo.hospitalinformationsystem.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("chat_history")
public class ChatHistory {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("patient_id")
    private String patientId;

    @TableField("chat_type")
    private String chatType;

    private String role;

    private String content;

    @TableField("create_time")
    private LocalDateTime createTime;
}
