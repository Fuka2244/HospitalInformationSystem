package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.dto.AppointmentRecommendation;
import com.hospitalinfo.hospitalinformationsystem.entity.Department;
import com.hospitalinfo.hospitalinformationsystem.entity.Doctor;
import com.hospitalinfo.hospitalinformationsystem.entity.DoctorSchedule;
import com.hospitalinfo.hospitalinformationsystem.mapper.DepartmentMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorMapper;
import com.hospitalinfo.hospitalinformationsystem.mapper.DoctorScheduleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI智能预约服务
 * 基于LangChain4j + Qwen，根据症状推荐科室、医生、时间段，并支持直接预约
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiAppointmentService {

    private final ChatLanguageModel chatModel;
    private final DepartmentMapper departmentMapper;
    private final DoctorMapper doctorMapper;
    private final DoctorScheduleMapper doctorScheduleMapper;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            你是一个专业的医疗导诊AI助手。根据用户描述的症状，从可用的排班中推荐最合适的科室、医生和就诊时间段。

            你必须严格按照以下JSON格式返回结果，不要包含任何其他文字说明：
            {
                "department": "推荐科室名称",
                "doctor": "推荐医生姓名",
                "recommendedDate": "推荐日期(格式:2024-01-15，必须从可用排班中选择)",
                "recommendedTime": "推荐时间段(格式:09:00-10:00，必须从可用排班中选择)",
                "reason": "推荐理由"
            }

            重要注意事项：
            1. 科室必须从提供的科室列表中选择
            2. 医生必须从提供的医生列表中选择，优先选择擅长领域与症状匹配的医生
            3. 【必须】recommendedDate 必须从【可用排班】中的日期选择
            4. 【必须】recommendedTime 必须从【可用排班】中的时间段选择
            5. 推荐时优先选择余号较多的时间段
            6. 推荐理由应包含症状分析、医生擅长领域匹配、以及选择的排班信息
            """;

    /**
     * 根据症状推荐预约
     */
    public AppointmentRecommendation recommendBySymptom(String symptom) {
        // 构建上下文：科室列表 + 医生列表 + 可用排班
        String context = buildDepartmentAndDoctorContext();

        String userPrompt = String.format("""
                当前可用科室和医生信息：
                %s

                患者症状描述：%s

                请推荐合适的科室、医生和就诊时间段（未来7天内的工作日）。
                """, context, symptom);

        String response = chatModel.generate(SYSTEM_PROMPT + "\n\n" + userPrompt);
        log.info("AI预约推荐原始响应: {}", response);

        return parseRecommendation(response);
    }

    /**
     * 根据AI推荐查询可用排班
     */
    public AppointmentRecommendation recommendWithSchedules(String symptom) {
        // 先查询未来7天内所有可用的排班
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);

        List<DoctorSchedule> availableSchedules = doctorScheduleMapper.selectList(
                new QueryWrapper<DoctorSchedule>()
                        .ge("schedule_date", startDate)
                        .le("schedule_date", endDate)
                        .eq("status", 1)
                        .orderByAsc("schedule_date", "time_slot"));

        // 过滤出未约满的排班
        List<DoctorSchedule> availableSlots = availableSchedules.stream()
                .filter(s -> s.getBookedCount() < s.getMaxPatients())
                .toList();

        if (availableSlots.isEmpty()) {
            AppointmentRecommendation fallback = new AppointmentRecommendation();
            fallback.setDepartment("全科");
            fallback.setDoctor("请前往导诊台咨询");
            fallback.setReason("未来7天内暂无可用排班，建议稍后重试或前往导诊台咨询");
            return fallback;
        }

        // 构建上下文：科室列表 + 医生列表 + 可用排班
        String context = buildDepartmentDoctorAndScheduleContext(availableSlots);

        String userPrompt = String.format("""
                当前可用科室、医生和排班信息：
                %s

                患者症状描述：%s

                请根据可用排班，推荐合适的科室、医生和就诊时间段。
                注意：必须从上述可用排班中选择推荐的时间和日期。
                """, context, symptom);

        String response = chatModel.generate(SYSTEM_PROMPT + "\n\n" + userPrompt);
        log.info("AI预约推荐原始响应: {}", response);

        AppointmentRecommendation recommendation = parseRecommendation(response);

        // 根据AI推荐的结果查询对应的ID
        Department department = departmentMapper.selectOne(
                new QueryWrapper<Department>().eq("name", recommendation.getDepartment()).eq("status", 1));

        Doctor doctor = doctorMapper.selectOne(
                new QueryWrapper<Doctor>().eq("name", recommendation.getDoctor())
                        .eq("status", 1)
                        .eq("department_id", department != null ? department.getId() : null));

        if (department != null) {
            recommendation.setDepartmentId(department.getId());
        }
        if (doctor != null) {
            recommendation.setDoctorId(doctor.getId());
        }

        // 查询该医生在推荐日期的所有可用排班
        if (doctor != null && recommendation.getRecommendedDate() != null) {
            LocalDate appointmentDate = LocalDate.parse(recommendation.getRecommendedDate());
            List<DoctorSchedule> doctorSchedules = availableSlots.stream()
                    .filter(s -> s.getDoctorId().equals(doctor.getId()) &&
                            s.getScheduleDate().equals(appointmentDate))
                    .toList();

            if (doctorSchedules.isEmpty()) {
                // AI推荐的日期无排班，查询该医生所有可用排班
                doctorSchedules = availableSlots.stream()
                        .filter(s -> s.getDoctorId().equals(doctor.getId()))
                        .toList();

                if (!doctorSchedules.isEmpty()) {
                    // 使用该医生的第一个可用排班
                    DoctorSchedule firstAvailable = doctorSchedules.get(0);
                    recommendation.setRecommendedDate(firstAvailable.getScheduleDate().toString());
                    recommendation.setRecommendedTime(firstAvailable.getTimeSlot());
                    recommendation.setReason(recommendation.getReason() +
                            String.format("\nAI推荐日期无排班，已为您调整为最近可用日期：%s %s",
                                    firstAvailable.getScheduleDate(), firstAvailable.getTimeSlot()));
                }
            } else {
                // AI推荐的时间段是否可用
                String recommendedTime = recommendation.getRecommendedTime();
                boolean exactTimeAvailable = doctorSchedules.stream()
                        .anyMatch(s -> s.getTimeSlot().equals(recommendedTime));

                if (exactTimeAvailable) {
                    recommendation.setReason(recommendation.getReason() +
                            String.format("\n已为您查询到%d个可用时间段，请选择合适的时段进行预约。", doctorSchedules.size()));
                } else {
                    // AI推荐的时间段不可用，使用第一个可用时间段
                    DoctorSchedule firstAvailable = doctorSchedules.get(0);
                    recommendation.setRecommendedTime(firstAvailable.getTimeSlot());
                    recommendation.setReason(recommendation.getReason() +
                            String.format("\nAI推荐时间段已约满，已为您调整为：%s %s。该医生共有%d个可用时间段。",
                                    firstAvailable.getScheduleDate(), firstAvailable.getTimeSlot(), doctorSchedules.size()));
                }
            }
        } else {
            recommendation.setReason(recommendation.getReason() +
                    "\n未找到推荐的医生或日期，请手动选择其他医生。");
        }

        return recommendation;
    }

    /**
     * 构建科室+医生+可用排班上下文
     */
    private String buildDepartmentDoctorAndScheduleContext(List<DoctorSchedule> availableSchedules) {
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

        sb.append("\n【可用排班】\n");
        for (DoctorSchedule schedule : availableSchedules) {
            Doctor doctor = doctors.stream()
                    .filter(d -> d.getId().equals(schedule.getDoctorId()))
                    .findFirst().orElse(null);

            if (doctor != null) {
                Department dept = departments.stream()
                        .filter(d -> d.getId().equals(doctor.getDepartmentId()))
                        .findFirst().orElse(null);

                String deptName = dept != null ? dept.getName() : "未知";
                int availableSlots = schedule.getMaxPatients() - schedule.getBookedCount();

                sb.append(String.format("- 日期:%s | 医生:%s(%s) | 时段:%s | 余号:%d\n",
                        schedule.getScheduleDate(),
                        doctor.getName(),
                        deptName,
                        schedule.getTimeSlot(),
                        availableSlots));
            }
        }

        return sb.toString();
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
