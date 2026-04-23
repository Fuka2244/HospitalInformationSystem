package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * 智能导诊对话响应
 */
@Data
public class TriageChatResponse {
    /** AI回复的消息 */
    private String reply;

    /** 对话是否完成（信息收集完毕，可以导诊） */
    private boolean completed;

    /** 当 completed=true 时，附带推荐结果 */
    private AppointmentRecommendation recommendation;

    public static TriageChatResponse ongoing(String reply) {
        TriageChatResponse r = new TriageChatResponse();
        r.setReply(reply);
        r.setCompleted(false);
        return r;
    }

    public static TriageChatResponse completed(String reply, AppointmentRecommendation recommendation) {
        TriageChatResponse r = new TriageChatResponse();
        r.setReply(reply);
        r.setCompleted(true);
        r.setRecommendation(recommendation);
        return r;
    }
}
