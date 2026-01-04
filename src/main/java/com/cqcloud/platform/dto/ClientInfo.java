package com.cqcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * 客户端连接信息
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientInfo {
    /**
     * 客户端ID
     */
    private String clientId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 连接时间
     */
    private LocalDateTime connectTime;

    /**
     * 最后心跳时间
     */
    private LocalDateTime lastHeartbeat;

    /**
     * 最后活动时间
     */
    private LocalDateTime lastActivity;

    /**
     * 加入的群组
     */
    private Set<String> groups;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 是否在线
     */
    private boolean online;
}