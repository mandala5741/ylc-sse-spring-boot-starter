package com.cqcloud.platform.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 群组事件
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEvent {
    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 群组ID
     */
    private String groupId;

    /**
     * 事件类型：join/leave/kick
     */
    private String action;

    /**
     * 事件时间
     */
    private LocalDateTime timestamp;

    /**
     * 附加数据
     */
    private Object payload;

    /**
     * 便利构造方法
     */
    public GroupEvent(String clientId, String groupId, String action) {
        this.clientId = clientId;
        this.groupId = groupId;
        this.action = action;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 创建加入群组事件
     */
    public static GroupEvent join(String clientId, String userId, String groupId) {
        return GroupEvent.builder()
                .clientId(clientId)
                .userId(userId)
                .groupId(groupId)
                .action("join")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建离开群组事件
     */
    public static GroupEvent leave(String clientId, String userId, String groupId) {
        return GroupEvent.builder()
                .clientId(clientId)
                .userId(userId)
                .groupId(groupId)
                .action("leave")
                .timestamp(LocalDateTime.now())
                .build();
    }
}