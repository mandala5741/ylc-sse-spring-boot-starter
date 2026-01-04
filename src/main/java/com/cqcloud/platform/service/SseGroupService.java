package com.cqcloud.platform.service;


import com.cqcloud.platform.dto.GroupEvent;
import com.cqcloud.platform.dto.SseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Set;

/**
 * 群组服务
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Service
@Slf4j
public class SseGroupService {

    public void sendToGroup(String groupId, Object message) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.forEach(userId -> {
                SseEmitter emitter = sseEmitters.get(userId);
                if (emitter != null) {
                    try {
                        // 使用 GroupEvent
                        GroupEvent groupEvent = GroupEvent.builder()
                                .groupId(groupId)
                                .action("message")
                                .timestamp(LocalDateTime.now())
                                .data(message)
                                .build();

                        SseEvent sseEvent = SseEvent.builder()
                                .event("group_message")
                                .data(groupEvent)
                                .id(generateEventId())
                                .build();

                        emitter.send(sseEvent.toSseEventBuilder());

                    } catch (IOException e) {
                        log.error("发送群组消息失败: userId={}, groupId={}", userId, groupId, e);
                        sseEmitters.remove(userId);
                    }
                }
            });
        }
    }

    public void joinGroup(String userId, String groupId) {
        groupMembers.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet())
                .add(userId);

        // 创建加入事件
        GroupEvent joinEvent = GroupEvent.join(
                getClientId(userId),
                userId,
                groupId
        );

        // 发送给群组所有成员
        SseEvent sseEvent = SseEvent.builder()
                .event("group_member_change")
                .data(joinEvent)
                .build();

        sendToGroup(groupId, sseEvent);
    }

    public void leaveGroup(String userId, String groupId) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.remove(userId);

            // 创建离开事件
            GroupEvent leaveEvent = GroupEvent.leave(
                    getClientId(userId),
                    userId,
                    groupId
            );

            // 发送给群组所有成员
            SseEvent sseEvent = SseEvent.builder()
                    .event("group_member_change")
                    .data(leaveEvent)
                    .build();

            sendToGroup(groupId, sseEvent);
        }
    }
}