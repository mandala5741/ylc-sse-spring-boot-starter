package com.cqcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConnectResponse {
    private String message;
    private LocalDateTime timestamp;
    private String sessionId;
    private Long heartbeatInterval;

    public ConnectResponse(String message, LocalDateTime timestamp) {
        this.message = message;
        this.timestamp = timestamp;
        this.sessionId = UUID.randomUUID().toString();
        this.heartbeatInterval = 30000L; // 默认30秒
    }
}
