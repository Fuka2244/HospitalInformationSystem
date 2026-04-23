package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import java.util.List;
import java.util.Map;

public interface IChatHistoryService {
    /** 保存一条聊天消息 */
    Result saveMessage(String patientId, String chatType, String role, String content);

    /** 批量保存聊天消息（对话结束后一次性保存） */
    Result saveMessages(String patientId, String chatType, List<Map<String, String>> messages);

    /** 获取指定类型的聊天历史 */
    Result listMessages(String patientId, String chatType);

    /** 清除指定类型的聊天历史 */
    Result clearMessages(String patientId, String chatType);
}
