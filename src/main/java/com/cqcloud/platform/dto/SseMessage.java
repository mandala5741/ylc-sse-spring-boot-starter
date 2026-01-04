package com.cqcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SSE消息包装类
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SseMessage {
    /**
     * 消息ID
     */
    private String id;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 消息内容
     */
    private Object data;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 来源
     */
    private String source;

    /**
     * 目标（用户ID/群组ID）
     */
    private String target;

    /**
     * 业务码
     */
    private Integer code;

    /**
     * 业务消息
     */
    private String message;

    /**
     * 创建成功消息
     */
    public static SseMessage success(Object data) {
        return SseMessage.builder()
                .type("success")
                .data(data)
                .timestamp(LocalDateTime.now())
                .code(200)
                .message("操作成功")
                .build();
    }

    /**
     * 创建错误消息
     */
    public static SseMessage error(String message) {
        return SseMessage.builder()
                .type("error")
                .timestamp(LocalDateTime.now())
                .code(500)
                .message(message)
                .build();
    }
}