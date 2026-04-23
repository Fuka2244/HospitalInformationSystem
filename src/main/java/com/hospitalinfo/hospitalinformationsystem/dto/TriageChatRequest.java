package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

import java.util.List;

/**
 * 智能导诊对话请求
 */
@Data
public class TriageChatRequest {
    /** 当前用户输入的消息 */
    private String message;

    /** 对话历史，每条包含 role(user/assistant) 和 content */
    private List<ChatMessageDto> history;
}
