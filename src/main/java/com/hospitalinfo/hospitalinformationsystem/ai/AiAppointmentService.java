package com.hospitalinfo.hospitalinformationsystem.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalinfo.hospitalinformationsystem.config.CacheConfig;
import com.hospitalinfo.hospitalinformationsystem.dto.AppointmentRecommendation;
import com.hospitalinfo.hospitalinformationsystem.dto.ChatMessageDto;
import com.hospitalinfo.hospitalinformationsystem.dto.DoctorWithScheduleDto;
import com.hospitalinfo.hospitalinformationsystem.dto.TriageChatResponse;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI智能预约服务
 * 基于LangChain4j + Qwen，根据症状推荐科室，查询该科室可用医生和排班，供患者选择
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

    /** AI只推荐科室的Prompt（不指定医生，避免AI幻觉） */
    private static final String DEPARTMENT_RECOMMEND_PROMPT = """
            你是一个专业的医疗导诊AI助手。根据用户描述的症状，推荐最合适的科室。

            你必须严格按照以下JSON格式返回结果，不要包含任何其他文字说明：
            {
                "department": "推荐科室名称",
                "reason": "推荐理由（包含症状分析和科室选择原因）"
            }

            重要注意事项：
            1. 科室必须从提供的科室列表中选择
            2. 只需要推荐科室，不需要推荐具体医生（系统会自动查询该科室的可用医生）
            3. 如果症状可能涉及多个科室，推荐最对口的那个
            """;

    /** 多轮导诊对话的System Prompt（对话式，AI直接推荐科室） */
    private static final String TRIAGE_CHAT_SYSTEM_PROMPT = """
            你是一个专业、友好的医疗导诊AI助手。你的任务是通过对话逐步了解患者的症状和就诊时间偏好，然后为其推荐最合适的科室。

            ## 对话流程规则：
            1. **第一步：询问症状** - 如果患者还没有描述症状，请温和地询问其症状。如果患者已经描述了症状，可以适当追问细节（如持续时间、严重程度、伴随症状等），但不要过于啰嗦。
            2. **第二步：询问时间偏好** - 在了解症状后，询问患者希望什么时间段就诊（如上午/下午、具体日期等）。
            3. **第三步：给出推荐** - 当症状和时间偏好都收集完毕后，用亲切自然的语气向患者推荐科室，并说明推荐理由。

            ## 回复格式规则：
            你必须在每次回复的最后附上一个JSON块来指示对话状态，格式如下：

            当还在收集信息时（还需要继续对话）：
            ```json
            {"completed": false}
            ```

            当信息已收集完毕，已经向患者推荐了科室时：
            ```json
            {"completed": true, "department": "推荐科室名称", "reason": "推荐理由"}
            ```

            ## 重要注意事项：
            - 语气亲切自然，像一位专业的导诊护士
            - 不要一次性问太多问题，每次只问1-2个关键问题
            - 如果患者描述的症状比较模糊，适当追问以明确
            - 如果患者提供了足够的信息，不要再追问，直接在对话中推荐科室并标记为completed
            - 推荐科室时要用对话的语气说出来，比如"根据您的症状，我建议您挂内科"，而不是只输出JSON
            - 科室必须从提供的科室列表中选择
            - 只推荐科室，不推荐具体医生
            - 每次回复必须以JSON状态块结尾，不要遗漏
            """;

    // ==================== 缓存方法 ====================

    /**
     * 缓存：查询所有启用科室
     */
    @Cacheable(value = CacheConfig.CACHE_DEPARTMENT, key = "'all'")
    public List<Department> getAllDepartmentsFromCache() {
        log.info("科室缓存未命中，查询数据库");
        return departmentMapper.selectList(
                new QueryWrapper<Department>().eq("status", 1));
    }

    /**
     * 缓存：查询指定科室下的所有启用医生
     */
    @Cacheable(value = CacheConfig.CACHE_DOCTOR, key = "'dept:' + #departmentId")
    public List<Doctor> getDoctorsByDepartmentFromCache(Long departmentId) {
        log.info("医生缓存未命中，查询数据库: departmentId={}", departmentId);
        return doctorMapper.selectList(
                new QueryWrapper<Doctor>().eq("department_id", departmentId).eq("status", 1));
    }

    /**
     * 缓存：查询某科室下所有医生的可用排班
     * 缓存Key: schedule:dept:{departmentId}
     * 注意：内部直接用mapper查医生，避免同Bean内调用@Cacheable失效
     */
    @Cacheable(value = CacheConfig.CACHE_SCHEDULE, key = "'dept:' + #departmentId")
    public List<DoctorSchedule> getDepartmentSchedulesFromCache(Long departmentId) {
        log.info("排班缓存未命中，查询数据库: departmentId={}", departmentId);
        // 直接查mapper，不走缓存方法（同Bean内@Cacheable代理不生效）
        List<Doctor> doctors = doctorMapper.selectList(
                new QueryWrapper<Doctor>().eq("department_id", departmentId).eq("status", 1));
        if (doctors.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> doctorIds = doctors.stream().map(Doctor::getId).toList();
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = LocalDate.now().plusDays(7);

        List<DoctorSchedule> schedules = doctorScheduleMapper.selectList(
                new QueryWrapper<DoctorSchedule>()
                        .in("doctor_id", doctorIds)
                        .ge("schedule_date", startDate)
                        .le("schedule_date", endDate)
                        .eq("status", 1)
                        .orderByAsc("schedule_date", "time_slot"));

        // 过滤未约满的排班
        return schedules.stream()
                .filter(s -> s.getBookedCount() < s.getMaxPatients())
                .collect(Collectors.toList());
    }

    // ==================== 核心推荐方法 ====================

    /**
     * 根据症状推荐预约（旧接口，兼容保留）
     */
    public AppointmentRecommendation recommendBySymptom(String symptom) {
        return recommendWithSchedules(symptom);
    }

    /**
     * 核心推荐流程（三步走 + Redis缓存）：
     * 第一步：AI只推荐科室（不指定医生，避免幻觉）
     * 第二步：从缓存查该科室下所有医生及其排班
     * 第三步：构建可选医生列表返回，让患者选择
     */
    public AppointmentRecommendation recommendWithSchedules(String symptom) {
        // ===== 第一步：AI推荐科室（不指定医生）=====
        List<Department> allDepts = getAllDepartmentsFromCache();
        String deptContext = buildDepartmentContext(allDepts);

        String deptPrompt = String.format("""
                当前医院可用的科室列表：
                %s

                患者症状描述：%s

                请推荐最合适的科室。
                """, deptContext, symptom);

        String deptResponse = chatModel.generate(DEPARTMENT_RECOMMEND_PROMPT + "\n\n" + deptPrompt);
        log.info("AI科室推荐原始响应: {}", deptResponse);

        // 解析推荐科室
        String recommendedDept = null;
        String recommendReason = "";
        try {
            var jsonNode = objectMapper.readTree(extractJson(deptResponse));
            if (jsonNode.has("department")) recommendedDept = jsonNode.get("department").asText();
            if (jsonNode.has("reason")) recommendReason = jsonNode.get("reason").asText();
        } catch (Exception e) {
            log.error("解析科室推荐结果失败: {}", e.getMessage());
            return buildFallbackRecommendation("AI推荐解析异常，建议前往导诊台");
        }

        // 匹配科室
        Department department = matchDepartment(allDepts, recommendedDept);

        if (department == null) {
            return buildFallbackRecommendation("未找到推荐科室「" + recommendedDept + "」，请前往导诊台咨询");
        }

        // ===== 第二步：从缓存查该科室下的医生和排班 =====
        List<Doctor> doctors = getDoctorsByDepartmentFromCache(department.getId());
        List<DoctorSchedule> allSchedules = getDepartmentSchedulesFromCache(department.getId());

        // ===== 第三步：构建可选医生列表 =====
        // 注意：allSchedules 来自缓存，必须深拷贝后再修改，避免污染缓存
        List<DoctorWithScheduleDto> availableDoctors = new ArrayList<>();
        for (Doctor doc : doctors) {
            List<DoctorSchedule> docSchedules = allSchedules.stream()
                    .filter(s -> s.getDoctorId().equals(doc.getId()))
                    .map(this::copySchedule) // 深拷贝，避免修改缓存对象
                    .toList();

            if (!docSchedules.isEmpty()) {
                // 填充排班中的医生名、科室ID和科室名
                for (DoctorSchedule schedule : docSchedules) {
                    schedule.setDoctorName(doc.getName());
                    schedule.setDepartmentId(department.getId());
                    schedule.setDepartmentName(department.getName());
                }

                DoctorWithScheduleDto dto = new DoctorWithScheduleDto();
                dto.setDoctorId(doc.getId());
                dto.setDoctorName(doc.getName());
                dto.setTitle(doc.getTitle());
                dto.setSpecialty(doc.getSpecialty());
                dto.setSchedules(docSchedules);
                availableDoctors.add(dto);
            }
        }

        // ===== 构建返回结果 =====
        return buildRecommendationResult(department, recommendReason, availableDoctors);
    }

    // ==================== 多轮对话 ====================

    /**
     * 多轮对话式智能导诊
     */
    public TriageChatResponse triageChat(String userMessage, List<ChatMessageDto> history) {
        // 构建对话上下文
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append(TRIAGE_CHAT_SYSTEM_PROMPT).append("\n\n");

        // 添加科室基础信息（从缓存）
        List<Department> allDepts = getAllDepartmentsFromCache();
        String context = buildDepartmentContext(allDepts);
        promptBuilder.append("当前医院可用的科室信息：\n").append(context).append("\n\n");

        // 添加历史对话
        if (history != null && !history.isEmpty()) {
            promptBuilder.append("===对话历史===\n");
            for (ChatMessageDto msg : history) {
                if ("user".equals(msg.getRole())) {
                    promptBuilder.append("患者：").append(msg.getContent()).append("\n");
                } else if ("assistant".equals(msg.getRole())) {
                    promptBuilder.append("导诊助手：").append(msg.getContent()).append("\n");
                }
            }
            promptBuilder.append("\n");
        }

        // 添加当前用户消息
        promptBuilder.append("患者：").append(userMessage).append("\n\n");
        promptBuilder.append("请根据以上对话，继续与患者沟通。记住在回复最后附上JSON状态块。\n");

        String response = chatModel.generate(promptBuilder.toString());
        log.info("导诊对话AI原始响应: {}", response);

        return parseTriageChatResponse(response, userMessage, history);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 构建科室上下文（从缓存获取数据）
     */
    private String buildDepartmentContext(List<Department> departments) {
        StringBuilder sb = new StringBuilder();
        for (Department dept : departments) {
            sb.append("- ").append(dept.getName());
            if (dept.getDescription() != null) {
                sb.append(": ").append(dept.getDescription());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 解析导诊对话AI的回复（对话式：直接从AI回复中提取推荐科室，无需二次AI调用）
     */
    private TriageChatResponse parseTriageChatResponse(String response, String userMessage, List<ChatMessageDto> history) {
        boolean completed = false;
        String recommendedDept = null;
        String recommendReason = "";
        String replyText = response;

        try {
            String jsonStr = extractJson(response);
            if (!jsonStr.isEmpty()) {
                var statusNode = objectMapper.readTree(jsonStr);
                if (statusNode.has("completed")) {
                    completed = statusNode.get("completed").asBoolean();
                }
                if (completed) {
                    if (statusNode.has("department")) recommendedDept = statusNode.get("department").asText();
                    if (statusNode.has("reason")) recommendReason = statusNode.get("reason").asText();
                }
            }

            // 去掉JSON块，只保留对话文本
            replyText = removeJsonBlocks(response);
        } catch (Exception e) {
            log.warn("解析导诊对话状态失败，默认为未完成: {}", e.getMessage());
        }

        if (!completed) {
            return TriageChatResponse.ongoing(replyText);
        }

        // 对话完成：从AI回复中提取科室，查库构建推荐结果（无需二次AI调用）
        log.info("导诊对话完成，AI推荐科室: {}, 理由: {}", recommendedDept, recommendReason);

        if (recommendedDept == null || recommendedDept.isEmpty()) {
            // AI未返回科室，用对话内容作为症状兜底走单次推荐
            String fullSymptom = buildFullSymptomFromHistory(userMessage, history);
            log.warn("对话完成但AI未返回科室，用症状兜底推荐: {}", fullSymptom);
            AppointmentRecommendation recommendation = recommendWithSchedules(fullSymptom);
            return TriageChatResponse.completed(replyText, recommendation);
        }

        AppointmentRecommendation recommendation = buildRecommendationByDepartment(recommendedDept, recommendReason);
        return TriageChatResponse.completed(replyText, recommendation);
    }

    /**
     * 根据科室名称构建推荐结果（从缓存查医生+排班，不需要AI调用）
     */
    private AppointmentRecommendation buildRecommendationByDepartment(String deptName, String reason) {
        List<Department> allDepts = getAllDepartmentsFromCache();

        Department department = matchDepartment(allDepts, deptName);

        if (department == null) {
            return buildFallbackRecommendation("未找到推荐科室「" + deptName + "」，请前往导诊台咨询");
        }

        // 从缓存查医生和排班（深拷贝，避免污染缓存）
        List<Doctor> doctors = getDoctorsByDepartmentFromCache(department.getId());
        List<DoctorSchedule> allSchedules = getDepartmentSchedulesFromCache(department.getId());

        // 构建可选医生列表
        List<DoctorWithScheduleDto> availableDoctors = new ArrayList<>();
        for (Doctor doc : doctors) {
            List<DoctorSchedule> docSchedules = allSchedules.stream()
                    .filter(s -> s.getDoctorId().equals(doc.getId()))
                    .map(this::copySchedule) // 深拷贝，避免修改缓存对象
                    .toList();

            if (!docSchedules.isEmpty()) {
                for (DoctorSchedule schedule : docSchedules) {
                    schedule.setDoctorName(doc.getName());
                    schedule.setDepartmentId(department.getId());
                    schedule.setDepartmentName(department.getName());
                }

                DoctorWithScheduleDto dto = new DoctorWithScheduleDto();
                dto.setDoctorId(doc.getId());
                dto.setDoctorName(doc.getName());
                dto.setTitle(doc.getTitle());
                dto.setSpecialty(doc.getSpecialty());
                dto.setSchedules(docSchedules);
                availableDoctors.add(dto);
            }
        }

        // 构建返回结果
        return buildRecommendationResult(department, reason, availableDoctors);
    }

    /**
     * 构建推荐结果（公共逻辑提取）
     */
    private AppointmentRecommendation buildRecommendationResult(Department department, String reason,
                                                                 List<DoctorWithScheduleDto> availableDoctors) {
        AppointmentRecommendation recommendation = new AppointmentRecommendation();
        recommendation.setDepartment(department.getName());
        recommendation.setDepartmentId(department.getId());
        recommendation.setReason(reason);
        recommendation.setAvailableDoctors(availableDoctors);

        if (availableDoctors.isEmpty()) {
            recommendation.setNeedChooseDoctor(false);
            recommendation.setReason(reason + "\n该科室未来7天暂无可用排班，建议稍后重试或前往导诊台咨询");
            return recommendation;
        }

        if (availableDoctors.size() == 1) {
            DoctorWithScheduleDto onlyDoctor = availableDoctors.get(0);
            DoctorSchedule firstSlot = onlyDoctor.getSchedules().get(0);
            recommendation.setDoctor(onlyDoctor.getDoctorName());
            recommendation.setDoctorId(onlyDoctor.getDoctorId());
            recommendation.setRecommendedDate(firstSlot.getScheduleDate().toString());
            recommendation.setRecommendedTime(firstSlot.getTimeSlot());
            recommendation.setNeedChooseDoctor(false);
            recommendation.setReason(reason + String.format(
                    "\n该科室仅一位医生有排班：%s（%s），已为您选择最早时段：%s %s",
                    onlyDoctor.getDoctorName(), onlyDoctor.getTitle(),
                    firstSlot.getScheduleDate(), firstSlot.getTimeSlot()));
        } else {
            recommendation.setNeedChooseDoctor(true);
            StringBuilder reasonBuilder = new StringBuilder(reason);
            reasonBuilder.append("\n该科室以下医生有可用排班，请选择：");
            for (DoctorWithScheduleDto doc : availableDoctors) {
                reasonBuilder.append(String.format("\n- %s（%s，擅长：%s），%d个可用时段",
                        doc.getDoctorName(), doc.getTitle(), doc.getSpecialty(),
                        doc.getSchedules().size()));
            }
            recommendation.setReason(reasonBuilder.toString());
        }

        return recommendation;
    }

    /**
     * 深拷贝DoctorSchedule对象，避免修改缓存中的原始对象
     */
    private DoctorSchedule copySchedule(DoctorSchedule source) {
        DoctorSchedule copy = new DoctorSchedule();
        copy.setId(source.getId());
        copy.setDoctorId(source.getDoctorId());
        copy.setScheduleDate(source.getScheduleDate());
        copy.setTimeSlot(source.getTimeSlot());
        copy.setMaxPatients(source.getMaxPatients());
        copy.setBookedCount(source.getBookedCount());
        copy.setStatus(source.getStatus());
        copy.setDoctorName(source.getDoctorName());
        copy.setDepartmentId(source.getDepartmentId());
        copy.setDepartmentName(source.getDepartmentName());
        return copy;
    }

    /**
     * 从AI回复中去除JSON代码块，只保留对话文本
     */
    private String removeJsonBlocks(String response) {
        String text = response;
        // 去掉 ```json ... ``` 块
        text = text.replaceAll("```json\\s*\\{[^}]*}\\s*```", "").trim();
        // 去掉 ``` ... ``` 块
        text = text.replaceAll("```\\s*\\{[^}]*}\\s*```", "").trim();
        // 清理残留的 ``` 标记
        text = text.replaceAll("```\\w*\\s*", "").trim();
        if (text.endsWith("```")) {
            text = text.substring(0, text.length() - 3).trim();
        }
        return text;
    }

    /**
     * 匹配科室：先精确匹配，再模糊匹配
     */
    private Department matchDepartment(List<Department> allDepts, String deptName) {
        // 精确匹配
        Department result = allDepts.stream()
                .filter(d -> d.getName().equals(deptName))
                .findFirst()
                .orElse(null);

        // 模糊匹配
        if (result == null) {
            result = allDepts.stream()
                    .filter(d -> d.getName().contains(deptName) || deptName.contains(d.getName()))
                    .findFirst()
                    .orElse(null);
        }

        return result;
    }

    /**
     * 从对话历史中提取综合症状描述
     */
    private String buildFullSymptomFromHistory(String currentUserMessage, List<ChatMessageDto> history) {
        StringBuilder symptomBuilder = new StringBuilder();
        if (history != null) {
            for (ChatMessageDto msg : history) {
                if ("user".equals(msg.getRole())) {
                    symptomBuilder.append(msg.getContent()).append("；");
                }
            }
        }
        symptomBuilder.append(currentUserMessage);
        return symptomBuilder.toString();
    }

    /**
     * 构建降级推荐结果
     */
    private AppointmentRecommendation buildFallbackRecommendation(String reason) {
        AppointmentRecommendation fallback = new AppointmentRecommendation();
        fallback.setDepartment("全科");
        fallback.setDoctor("请前往导诊台咨询");
        fallback.setReason(reason);
        return fallback;
    }

    /**
     * 从AI响应中提取JSON字符串
     */
    private String extractJson(String response) {
        String json = response;
        if (response.contains("```json")) {
            json = response.substring(response.indexOf("```json") + 7);
            json = json.substring(0, json.indexOf("```"));
        } else if (response.contains("```")) {
            json = response.substring(response.indexOf("```") + 3);
            json = json.substring(0, json.indexOf("```"));
        }
        return json.trim();
    }

    /**
     * 解析AI返回的JSON为推荐结果（旧接口兼容）
     */
    private AppointmentRecommendation parseRecommendation(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, AppointmentRecommendation.class);
        } catch (Exception e) {
            log.error("解析AI推荐结果失败: {}", e.getMessage());
            return buildFallbackRecommendation("AI推荐解析异常，建议前往导诊台");
        }
    }
}
