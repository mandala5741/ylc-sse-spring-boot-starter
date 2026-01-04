package com.cqcloud.platform.service;

import com.cqcloud.platform.dto.GroupEvent;
import com.cqcloud.platform.dto.NotificationMessage;
import com.cqcloud.platform.dto.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 群组消息服务
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupMessageService {

    private final GroupMembersManager groupMembersManager;
    private final Map<String, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final Map<String, String> clientToUser = new ConcurrentHashMap<>();

    /**
     * 发送消息到群组
     */
    public void sendToGroup(String groupId, NotificationMessage message) {
        Set<String> members = groupMembersManager.getGroupMembers(groupId);

        if (members == null || members.isEmpty()) {
            log.warn("群组为空或不存在: groupId={}", groupId);
            return;
        }

        // 构建群组消息事件
        SseEvent sseEvent = SseEvent.builder()
                .event("group_message")
                .data(message)
                .id(generateEventId())
                .build();

        // 发送给所有成员
        members.forEach(clientId -> {
            SseEmitter emitter = sseEmitters.get(clientId);
            if (emitter != null) {
                try {
                    emitter.send(sseEvent.toSseEventBuilder());
                    log.debug("发送群组消息成功: groupId={}, clientId={}", groupId, clientId);
                } catch (IOException e) {
                    log.error("发送群组消息失败: clientId={}", clientId, e);
                    removeClient(clientId);
                }
            }
        });
    }

    /**
     * 发送群组成员变更事件
     */
    public void sendMemberChangeEvent(String groupId, GroupEvent groupEvent) {
        Set<String> members = groupMembersManager.getGroupMembers(groupId);

        if (members == null) {
            return;
        }

        // 构建成员变更事件
        SseEvent sseEvent = SseEvent.builder()
                .event("group_member_change")
                .data(groupEvent)
                .id(generateEventId())
                .build();

        // 发送给所有成员
        members.forEach(clientId -> {
            SseEmitter emitter = sseEmitters.get(clientId);
            if (emitter != null) {
                try {
                    emitter.send(sseEvent.toSseEventBuilder());
                } catch (IOException e) {
                    log.error("发送成员变更事件失败", e);
                    removeClient(clientId);
                }
            }
        });
    }

    /**
     * 广播消息到所有群组
     */
    public void broadcastToAllGroups(NotificationMessage message) {
        Set<String> allGroups = new HashSet<>(groupMembersManager.getAllGroups());

        allGroups.forEach(groupId -> {
            sendToGroup(groupId, message);
        });
    }

    /**
     * 发送系统通知到群组
     */
    public void sendSystemNotification(String groupId, String content) {
        NotificationMessage systemMsg = NotificationMessage.builder()
                .type("system")
                .title("系统通知")
                .content(content)
                .sender("system")
                .groupId(groupId)
                .priority("HIGH")
                .createdAt(LocalDateTime.now())
                .build();

        sendToGroup(groupId, systemMsg);
    }

    /**
     * 获取群组在线成员
     */
    public Set<String> getOnlineGroupMembers(String groupId) {
        Set<String> allMembers = groupMembersManager.getGroupMembers(groupId);
        Set<String> onlineMembers = ConcurrentHashMap.newKeySet();

        allMembers.forEach(clientId -> {
            if (sseEmitters.containsKey(clientId)) {
                onlineMembers.add(clientId);
            }
        });

        return onlineMembers;
    }

    /**
     * 添加客户端连接
     */
    public void addClientConnection(String clientId, String userId, SseEmitter emitter) {
        sseEmitters.put(clientId, emitter);
        clientToUser.put(clientId, userId);

        // 设置回调
        emitter.onCompletion(() -> removeClient(clientId));
        emitter.onTimeout(() -> removeClient(clientId));
    }

    /**
     * 移除客户端
     */
    public void removeClient(String clientId) {
        SseEmitter emitter = sseEmitters.remove(clientId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // 忽略异常
            }
        }
        clientToUser.remove(clientId);
    }

    /**
     * 获取客户端连接
     */
    public SseEmitter getClientEmitter(String clientId) {
        return sseEmitters.get(clientId);
    }

    /**
     * 获取所有在线客户端
     */
    public Set<String> getOnlineClients() {
        return sseEmitters.keySet();
    }

    /**
     * 生成事件ID
     */
    private String generateEventId() {
        return String.valueOf(System.currentTimeMillis());
    }
}