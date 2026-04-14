package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.AppointmentRecommendation;
import com.hospitalinfo.hospitalinformationsystem.entity.Department;
import com.hospitalinfo.hospitalinformationsystem.entity.Doctor;
import com.hospitalinfo.hospitalinformationsystem.mapper.DepartmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI智能预约服务
 * 基于LangChain4j + Qwen，根据症状推荐科室、医生、时间段
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAppointmentService {

    private final ChatLanguageModel chatModel;
    private final DepartmentMapper departmentMapper;
    private final DoctorMapper doctorMapper;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的医疗导诊AI助手。根据用户描述的症状，推荐最合适的科室、医生和就诊时间段。
            
            你必须严格按照以下JSON格式返回结果，不要包含任何其他文字说明：
            {
                "department": "推荐科室名称",
                "doctor": "推荐医生姓名",
                "recommendedTime": "推荐时间段(如:上午09:00-10:00)",
                "reason": "推荐理由"
            }
            
            注意事项：
            1. 科室必须从提供的科室列表中选择
            2. 医生必须从提供的医生列表中选择，优先选择擅长领域与症状匹配的医生
            3. 时间段建议选择工作日上午
            4. 推荐理由应包含症状分析和选择依据
            """;

    /**
     * 根据症状推荐预约
     */
    public AppointmentRecommendation recommendBySymptom(String symptom) {
        // 构建上下文：科室列表 + 医生列表
        String context = buildDepartmentAndDoctorContext();

        String userPrompt = String.format("""
                当前可用科室和医生信息：
                %s
                
                患者症状描述：%s
                
                请推荐合适的科室、医生和就诊时间段。
                """, context, symptom);

        String response = chatModel.generate(SYSTEM_PROMPT + "\n\n" + userPrompt);
        log.info("AI预约推荐原始响应: {}", response);

        return parseRecommendation(response);
    }

    /**
     * 构建科室+医生上下文（用于RAG替代方案）
     */
    private String buildDepartmentAndDoctorContext() {
        List<Department> departments = departmentMapper.selectList(
                new QueryWrapper<Department>().eq("status", 1));
        List<Doctor> doctors = doctorMapper.selectList(
                new QueryWrapper<Doctor>().eq("status", 1));

        StringBuilder sb = new StringBuilder();
        sb.append("【科室列表】\n");
        for (Department dept : departments) {
            sb.append("- ").append(dept.getName()).append(": ").append(dept.getDescription()).append("\n");
        }
        sb.append("\n【医生列表】\n");
        for (Doctor doc : doctors) {
            String deptName = departments.stream()
                    .filter(d -> d.getId().equals(doc.getDepartmentId()))
                    .map(Department::getName)
                    .findFirst().orElse("未知科室");
            sb.append("- ").append(doc.getName())
              .append(" | 科室: ").append(deptName)
              .append(" | 职称: ").append(doc.getTitle())
              .append(" | 擅长: ").append(doc.getSpecialty())
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析AI返回的JSON为推荐结果
     */
    private AppointmentRecommendation parseRecommendation(String response) {
        try {
            // 提取JSON部分（AI可能返回markdown代码块包裹的JSON）
            String json = response;
            if (response.contains("```json")) {
                json = response.substring(response.indexOf("```json") + 7);
                json = json.substring(0, json.indexOf("```"));
            } else if (response.contains("```")) {
                json = response.substring(response.indexOf("```") + 3);
                json = json.substring(0, json.indexOf("```"));
            }
            json = json.trim();
            return objectMapper.readValue(json, AppointmentRecommendation.class);
        } catch (Exception e) {
            log.error("解析AI推荐结果失败: {}", e.getMessage());
            AppointmentRecommendation fallback = new AppointmentRecommendation();
            fallback.setDepartment("全科");
            fallback.setDoctor("请前往导诊台咨询");
            fallback.setRecommendedTime("工作日上午");
            fallback.setReason("AI推荐解析异常，建议前往导诊台");
            return fallback;
        }
    }
}
