package com.hospitalinfo.hospitalinformationsystem.dto;

import lombok.Data;

/**
 * AI费用解释对话响应
 */
@Data
public class BillingChatResponse {
    /** AI回复的消息 */
    private String reply;

    /** 对话是否完成（信息收集完毕，可以给出解释） */
    private boolean completed;

    /** 当 completed=true 时，附带费用解释结果 */
    private BillingExplanation explanation;

    public static BillingChatResponse ongoing(String reply) {
        BillingChatResponse r = new BillingChatResponse();
        r.setReply(reply);
        r.setCompleted(false);
        return r;
    }

    public static BillingChatResponse completed(String reply, BillingExplanation explanation) {
        BillingChatResponse r = new BillingChatResponse();
        r.setReply(reply);
        r.setCompleted(true);
        r.setExplanation(explanation);
        return r;
    }
}
