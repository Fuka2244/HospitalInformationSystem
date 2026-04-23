package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.ChatHistory;
import com.hospitalinfo.hospitalinformationsystem.mapper.ChatHistoryMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IChatHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatHistoryServiceImpl implements IChatHistoryService {

    private final ChatHistoryMapper chatHistoryMapper;

    @Override
    public Result saveMessage(String patientId, String chatType, String role, String content) {
        ChatHistory history = new ChatHistory();
        history.setPatientId(patientId);
        history.setChatType(chatType);
        history.setRole(role);
        history.setContent(content);
        history.setCreateTime(LocalDateTime.now());
        chatHistoryMapper.insert(history);
        return Result.ok();
    }

    @Override
    @Transactional
    public Result saveMessages(String patientId, String chatType, List<Map<String, String>> messages) {
        LocalDateTime now = LocalDateTime.now();
        for (Map<String, String> msg : messages) {
            ChatHistory history = new ChatHistory();
            history.setPatientId(patientId);
            history.setChatType(chatType);
            history.setRole(msg.get("role"));
            history.setContent(msg.get("content"));
            history.setCreateTime(now);
            chatHistoryMapper.insert(history);
        }
        return Result.ok();
    }

    @Override
    public Result listMessages(String patientId, String chatType) {
        QueryWrapper<ChatHistory> wrapper = new QueryWrapper<ChatHistory>()
                .eq("patient_id", patientId)
                .eq("chat_type", chatType)
                .orderByAsc("create_time");
        List<ChatHistory> records = chatHistoryMapper.selectList(wrapper);
        List<ChatMessageDto> messages = new ArrayList<>();
        for (ChatHistory record : records) {
            messages.add(new ChatMessageDto(record.getRole(), record.getContent()));
        }
        return Result.ok(messages);
    }

    @Override
    @Transactional
    public Result clearMessages(String patientId, String chatType) {
        QueryWrapper<ChatHistory> wrapper = new QueryWrapper<ChatHistory>()
                .eq("patient_id", patientId)
                .eq("chat_type", chatType);
        chatHistoryMapper.delete(wrapper);
        return Result.ok();
    }
}
