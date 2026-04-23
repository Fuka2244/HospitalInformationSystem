package com.hospitalinfo.hospitalinformationsystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiMedicineService;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.MedicineChatResponse;
import com.hospitalinfo.hospitalinformationsystem.dto.MedicineRecommendation;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import com.hospitalinfo.hospitalinformationsystem.service.IMedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MedicineServiceImpl implements IMedicineService {

    private final MedicineMapper medicineMapper;
    private final AiMedicineService aiMedicineService;

    @Override
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
}
