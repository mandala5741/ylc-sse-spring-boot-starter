package com.cqcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知消息
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessage {
    /**
     * 消息ID
     */
    private String id;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 发送者
     */
    private String sender;

    /**
     * 接收者
     */
    private String receiver;

    /**
     * 群组ID（如果是群组消息）
     */
    private String groupId;

    /**
     * 业务数据
     */
    private Object data;

    /**
     * 附加信息
     */
    private Map<String, Object> extra;

    /**
     * 优先级：HIGH/MEDIUM/LOW
     */
    private String priority;

    /**
     * 是否持久化
     */
    private boolean persistent;

    /**
     * 过期时间
     */
    private LocalDateTime expireAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}