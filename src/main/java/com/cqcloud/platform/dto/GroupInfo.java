package com.cqcloud.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * 群组信息
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInfo {
    private String groupId;
    private String groupName;
    private String creator;
    private Date createTime;
    private Integer maxMembers;
    private Map<String, Object> metadata;
    private Set<String> administrators;
}
