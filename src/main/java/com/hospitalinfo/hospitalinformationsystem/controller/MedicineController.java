package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.AsyncTaskResult;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.TriageChatRequest;
import com.hospitalinfo.hospitalinformationsystem.service.IAsyncTaskService;
import com.hospitalinfo.hospitalinformationsystem.service.IMedicineService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 药品信息查询控制器（含AI推荐）
 */
@RestController
@RequestMapping("/medicine")
@RequiredArgsConstructor
public class MedicineController {

    private final IMedicineService medicineService;
    private final IAsyncTaskService asyncTaskService;

    /**
     * 分页查询药品列表
     * GET /medicine/list?keyword=xxx&category=xxx&page=1&size=10
     */
    @GetMapping("/list")
    public Result listMedicines(@RequestParam(required = false) String keyword,
                                 @RequestParam(required = false) String category,
                                 @RequestParam(defaultValue = "1") Integer page,
                                 @RequestParam(defaultValue = "10") Integer size) {
        return medicineService.listMedicines(keyword, category, page, size);
    }

    /**
     * 获取药品详情
     * GET /medicine/{id}
     */
    @GetMapping("/{id}")
    public Result getMedicineDetail(@PathVariable Long id) {
        return medicineService.getMedicineDetail(id);
    }

    /**
     * AI药物推荐
     * POST /medicine/ai-recommend
     * 请求体: { "symptom": "头痛发热" }
     */
    @PostMapping("/ai-recommend")
    public Result aiRecommend(@RequestBody Map<String, String> body) {
        String symptom = body.get("symptom");
        if (symptom == null || symptom.isEmpty()) {
            return Result.fail("请描述您的症状");
        }
        return medicineService.aiRecommendMedicine(symptom);
    }

    /**
     * AI多轮对话式药物推荐（同步）
     * POST /medicine/ai-chat
     * 请求体: { "message": "我头痛", "history": [{"role":"user","content":"..."},{"role":"assistant","content":"..."}] }
     */
    @PostMapping("/ai-chat")
    public Result aiMedicineChat(@RequestBody TriageChatRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Result.fail("请输入您的消息");
        }
        return medicineService.aiMedicineChat(request.getMessage(), request.getHistory());
    }

    /**
     * AI多轮对话式药物推荐（异步，提交任务返回taskId）
     * POST /medicine/ai-chat-async
     */
    @PostMapping("/ai-chat-async")
    public Result aiMedicineChatAsync(@RequestBody TriageChatRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return Result.fail("请输入您的消息");
        }
        return medicineService.aiMedicineChatAsync(request.getMessage(), request.getHistory());
    }

    /**
     * 查询异步任务结果
     * GET /medicine/ai-task/{taskId}
     */
    @GetMapping("/ai-task/{taskId}")
    public Result getAiTaskResult(@PathVariable String taskId, HttpSession session) {
        String currentPatientId = (String) session.getAttribute("account");
        AsyncTaskResult taskResult = asyncTaskService.getTaskResult(taskId);
        // 权限验证：只有任务所属患者才能查看结果
        if (taskResult != null && taskResult.getPatientId() != null
                && !taskResult.getPatientId().equals(currentPatientId)) {
            Object role = session.getAttribute("role");
            if (role == null || (!"admin".equals(role) && !"doctor".equals(role))) {
                return Result.fail("无权查看该任务结果");
            }
        }
        return Result.ok(taskResult);
    }
}
