package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

import java.util.List;

/**
 * AI药品推荐对话响应
 */
@Data
public class MedicineChatResponse {
    /** AI回复的消息 */
    private String reply;

    /** 对话是否完成（信息收集完毕，可以给出推荐） */
    private boolean completed;

    /** 当 completed=true 时，附带推荐结果 */
    private List<MedicineRecommendation> recommendations;

    public static MedicineChatResponse ongoing(String reply) {
        MedicineChatResponse r = new MedicineChatResponse();
        r.setReply(reply);
        r.setCompleted(false);
        return r;
    }

    public static MedicineChatResponse completed(String reply, List<MedicineRecommendation> recommendations) {
        MedicineChatResponse r = new MedicineChatResponse();
        r.setReply(reply);
        r.setCompleted(true);
        r.setRecommendations(recommendations);
        return r;
    }
}
