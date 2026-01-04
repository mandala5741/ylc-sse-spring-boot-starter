package com.cqcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE事件
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseEvent {
    private String id;
    private String event;
    private Object data;
    private Long retry;
    private String comment;

    public SseEmitter.SseEventBuilder toSseEventBuilder() {
        SseEmitter.SseEventBuilder builder = SseEmitter.event();
        if (id != null) builder.id(id);
        if (event != null) builder.name(event);
        if (data != null) builder.data(data);
        if (retry != null) builder.reconnectTime(retry);
        if (comment != null) builder.comment(comment);
        return builder;
    }
}





