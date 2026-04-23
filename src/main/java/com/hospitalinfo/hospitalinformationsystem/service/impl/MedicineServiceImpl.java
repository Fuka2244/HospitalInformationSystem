package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.ai.AiMedicineService;
import com.hospitalinfo.hospitalinformationsystem.config.CacheConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.*;
import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IAsyncTaskService;
import com.hospitalinfo.hospitalinformationsystem.service.IMedicineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements IMedicineService {

    private final MedicineMapper medicineMapper;
    private final AiMedicineService aiMedicineService;
    private final IAsyncTaskService asyncTaskService;
    private final ObjectMapper objectMapper;

    @Override
    @Cacheable(value = CacheConfig.CACHE_MEDICINE, key = "'list:' + (#keyword ?: '') + ':' + (#category ?: '') + ':' + #page + ':' + #size")
    public Result listMedicines(String keyword, String category, Integer page, Integer size) {
        Page<Medicine> pageParam = new Page<>(page, size);
        QueryWrapper<Medicine> wrapper = new QueryWrapper<Medicine>().eq("status", 1);

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("name", keyword)
                    .or().like("generic_name", keyword)
                    .or().like("efficacy", keyword));
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        wrapper.orderByAsc("name");

        Page<Medicine> result = medicineMapper.selectPage(pageParam, wrapper);
        return Result.ok(result.getRecords(), result.getTotal());
    }

    @Override
    @Cacheable(value = CacheConfig.CACHE_MEDICINE, key = "'detail:' + #medicineId")
    public Result getMedicineDetail(Long medicineId) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            return Result.fail("药品不存在");
        }
        return Result.ok(medicine);
    }

    @Override
    public Result aiRecommendMedicine(String symptom) {
        List<MedicineRecommendation> recommendations = aiMedicineService.recommendBySymptom(symptom);
        return Result.ok(recommendations);
    }

    @Override
    public Result aiMedicineChat(String message, List<ChatMessageDto> history) {
        MedicineChatResponse chatResponse = aiMedicineService.medicineChat(message, history);
        return Result.ok(chatResponse);
    }

    /**
     * 异步提交AI药品对话任务，返回taskId
     */
    @Override
    public Result aiMedicineChatAsync(String message, List<ChatMessageDto> history) {
        try {
            AsyncTaskRequest request = new AsyncTaskRequest();
            request.setTaskType("MEDICINE_CHAT");
            request.setMessage(message);
            if (history != null && !history.isEmpty()) {
                request.setHistoryJson(objectMapper.writeValueAsString(history));
            }

            String taskId = asyncTaskService.submitTask(request);
            log.info("AI药品对话异步任务已提交: taskId={}", taskId);
            return Result.ok(taskId);
        } catch (Exception e) {
            log.error("提交AI药品对话异步任务失败: {}", e.getMessage(), e);
            return Result.fail("提交异步任务失败");
        }
    }
}
