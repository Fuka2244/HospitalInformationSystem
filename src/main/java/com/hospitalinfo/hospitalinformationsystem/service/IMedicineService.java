package com.hospitalinfo.hospitalinformationsystem.service;

import com.hospitalinfo.hospitalinformationsystem.dto.MedicineRecommendation;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;

import java.util.List;

/**
 * 药品信息查询服务接口
 */
public interface IMedicineService {

    /** 分页查询药品列表 */
    Result listMedicines(String keyword, String category, Integer page, Integer size);

    /** 获取药品详情 */
    Result getMedicineDetail(Long medicineId);

    /** AI药物推荐 */
    Result aiRecommendMedicine(String symptom);
}
