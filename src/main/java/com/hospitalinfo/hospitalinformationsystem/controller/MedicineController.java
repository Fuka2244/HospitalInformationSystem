package com.hospitalinfo.hospitalinformationsystem.controller;

import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.dto.TriageChatRequest;
import com.hospitalinfo.hospitalinformationsystem.service.IMedicineService;
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
     * AI多轮对话式药物推荐
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
}
