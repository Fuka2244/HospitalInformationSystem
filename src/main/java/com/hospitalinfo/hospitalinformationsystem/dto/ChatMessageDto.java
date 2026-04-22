package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * 对话消息DTO
 */
@Data
public class ChatMessageDto {
    /** 角色: user 或 assistant */
    private String role;
    /** 消息内容 */
    private String content;

    public ChatMessageDto() {}

    public ChatMessageDto(String role, String content) {
        this.role = role;
        this.content = content;
    }
}
