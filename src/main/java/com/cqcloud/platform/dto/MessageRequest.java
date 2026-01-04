package com.cqcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private String content;
    private String type;
    private String target;
    private Map<String, Object> payload;
    private Integer priority;
    public String title;
    public Map<String, Object> extra;
}