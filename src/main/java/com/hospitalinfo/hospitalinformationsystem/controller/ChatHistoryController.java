package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.service.IChatHistoryService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat-history")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final IChatHistoryService chatHistoryService;

    /** 获取指定类型的聊天历史 */
    @GetMapping("/list")
    public Result listMessages(@RequestParam String chatType, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        if (patientId == null) {
            return Result.fail("未登录");
        }
        return chatHistoryService.listMessages(patientId, chatType);
    }

    /** 保存一条聊天消息 */
    @PostMapping("/save")
    public Result saveMessage(@RequestBody Map<String, String> body, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        if (patientId == null) {
            return Result.fail("未登录");
        }
        String chatType = body.get("chatType");
        String role = body.get("role");
        String content = body.get("content");
        if (chatType == null || role == null || content == null) {
            return Result.fail("参数不完整");
        }
        return chatHistoryService.saveMessage(patientId, chatType, role, content);
    }

    /** 批量保存聊天消息 */
    @PostMapping("/save-batch")
    public Result saveMessages(@RequestBody Map<String, Object> body, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        if (patientId == null) {
            return Result.fail("未登录");
        }
        String chatType = (String) body.get("chatType");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) body.get("messages");
        if (chatType == null || messages == null) {
            return Result.fail("参数不完整");
        }
        return chatHistoryService.saveMessages(patientId, chatType, messages);
    }

    /** 清除指定类型的聊天历史 */
    @DeleteMapping("/clear")
    public Result clearMessages(@RequestParam String chatType, HttpSession session) {
        String patientId = (String) session.getAttribute("account");
        if (patientId == null) {
            return Result.fail("未登录");
        }
        return chatHistoryService.clearMessages(patientId, chatType);
    }
}
