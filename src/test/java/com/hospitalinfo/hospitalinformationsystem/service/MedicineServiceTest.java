package com.hospitalinfo.hospitalinformationsystem.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hospitalinfo.hospitalinformationsystem.ai.AiMedicineService;
import com.hospitalinfo.hospitalinformationsystem.dto.MedicineRecommendation;
import com.hospitalinfo.hospitalinformationsystem.dto.Result;
import com.hospitalinfo.hospitalinformationsystem.entity.Medicine;
import com.hospitalinfo.hospitalinformationsystem.mapper.MedicineMapper;
import com.hospitalinfo.hospitalinformationsystem.service.impl.MedicineServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 药品信息查询模块单元测试
 * 覆盖：分页查询药品列表、药品详情、AI药物推荐
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("模块3 - 药品信息查询")
class MedicineServiceTest {

    @Mock private MedicineMapper medicineMapper;
    @Mock private AiMedicineService aiMedicineService;

    @InjectMocks
    private MedicineServiceImpl medicineService;

    private Medicine mockMedicine;

    @BeforeEach
    void setUp() {
        mockMedicine = new Medicine();
        mockMedicine.setId(1L);
        mockMedicine.setName("阿莫西林胶囊");
        mockMedicine.setGenericName("阿莫西林");
        mockMedicine.setCategory("抗生素");
        mockMedicine.setSpecification("0.5g*24粒");
        mockMedicine.setManufacturer("某药厂");
        mockMedicine.setIngredients("阿莫西林");
        mockMedicine.setEfficacy("用于敏感菌所致的各种感染");
        mockMedicine.setSideEffects("恶心、呕吐、腹泻");
        mockMedicine.setContraindications("青霉素过敏者禁用");
        mockMedicine.setPrice(new BigDecimal("15.50"));
        mockMedicine.setStock(500);
        mockMedicine.setStatus(1);
    }

    // ==================== 分页查询药品列表 ====================

    @Nested
    @DisplayName("分页查询药品列表")
    class ListMedicinesTests {

        @Test
        @DisplayName("无条件查询 - 返回全部药品")
        void listAllMedicines() {
            Page<Medicine> page = new Page<>(1, 10);
            page.setRecords(List.of(mockMedicine));
            page.setTotal(1L);
            when(medicineMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = medicineService.listMedicines(null, null, 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(1L, result.getTotal());
        }

        @Test
        @DisplayName("按关键词搜索 - 名称/通用名/功效模糊匹配")
        void listMedicinesByKeyword() {
            Page<Medicine> page = new Page<>(1, 10);
            page.setRecords(List.of(mockMedicine));
            page.setTotal(1L);
            when(medicineMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = medicineService.listMedicines("阿莫西林", null, 1, 10);

            assertTrue(result.getSuccess());
            verify(medicineMapper).selectPage(any(Page.class), any(QueryWrapper.class));
        }

        @Test
        @DisplayName("按分类筛选")
        void listMedicinesByCategory() {
            Page<Medicine> page = new Page<>(1, 10);
            page.setRecords(List.of(mockMedicine));
            page.setTotal(1L);
            when(medicineMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = medicineService.listMedicines(null, "抗生素", 1, 10);

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("关键词+分类组合查询")
        void listMedicinesByKeywordAndCategory() {
            Page<Medicine> page = new Page<>(1, 10);
            page.setRecords(List.of(mockMedicine));
            page.setTotal(1L);
            when(medicineMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = medicineService.listMedicines("阿莫", "抗生素", 1, 10);

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("查询结果为空")
        void listMedicinesEmpty() {
            Page<Medicine> page = new Page<>(1, 10);
            page.setRecords(List.of());
            page.setTotal(0L);
            when(medicineMapper.selectPage(any(Page.class), any(QueryWrapper.class))).thenReturn(page);

            Result result = medicineService.listMedicines("不存在的药品", null, 1, 10);

            assertTrue(result.getSuccess());
            assertEquals(0L, result.getTotal());
        }
    }

    // ==================== 药品详情 ====================

    @Nested
    @DisplayName("获取药品详情")
    class GetMedicineDetailTests {

        @Test
        @DisplayName("成功获取药品详情")
        void getDetailSuccess() {
            when(medicineMapper.selectById(1L)).thenReturn(mockMedicine);

            Result result = medicineService.getMedicineDetail(1L);

            assertTrue(result.getSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("药品不存在 - 返回失败")
        void getDetailNotFound() {
            when(medicineMapper.selectById(999L)).thenReturn(null);

            Result result = medicineService.getMedicineDetail(999L);

            assertFalse(result.getSuccess());
            assertEquals("药品不存在", result.getErrorMsg());
        }
    }

    // ==================== AI药物推荐 ====================

    @Nested
    @DisplayName("AI药物推荐")
    class AiRecommendMedicineTests {

        @Test
        @DisplayName("AI推荐成功 - 返回推荐药品列表")
        void aiRecommendSuccess() {
            MedicineRecommendation rec = new MedicineRecommendation();
            rec.setMedicineName("阿莫西林胶囊");
            rec.setReason("该药适用于上呼吸道感染，与您的症状匹配");
            rec.setDosage("口服，一次0.5g，每8小时一次");
            rec.setPrecautions("青霉素过敏者禁用；不可与酒精同服");

            when(aiMedicineService.recommendBySymptom("喉咙痛发烧")).thenReturn(List.of(rec));

            Result result = medicineService.aiRecommendMedicine("喉咙痛发烧");

            assertTrue(result.getSuccess());
            assertNotNull(result.getData());
        }

        @Test
        @DisplayName("AI推荐 - 多种药品推荐")
        void aiRecommendMultiple() {
            MedicineRecommendation rec1 = new MedicineRecommendation();
            rec1.setMedicineName("阿莫西林胶囊");
            MedicineRecommendation rec2 = new MedicineRecommendation();
            rec2.setMedicineName("布洛芬缓释胶囊");

            when(aiMedicineService.recommendBySymptom("发热头痛")).thenReturn(List.of(rec1, rec2));

            Result result = medicineService.aiRecommendMedicine("发热头痛");

            assertTrue(result.getSuccess());
        }

        @Test
        @DisplayName("AI推荐 - 无匹配药品返回空列表")
        void aiRecommendEmpty() {
            when(aiMedicineService.recommendBySymptom("罕见症状")).thenReturn(List.of());

            Result result = medicineService.aiRecommendMedicine("罕见症状");

            assertTrue(result.getSuccess());
        }
    }
}
