package com.cqcloud.platform.handler;

import com.cqcloud.platform.dto.*;
import com.cqcloud.platform.service.GroupMembersManager;
import com.cqcloud.platform.service.GroupMessageService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 群组SSE处理
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Slf4j
@RestController
@RequestMapping("/api/sse/group")
@RequiredArgsConstructor
public class GroupSseHandler {

    private final GroupMembersManager groupMembersManager;
    private final GroupMessageService groupMessageService;

    /**
     * 连接SSE并加入群组
     */
    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter connectAndJoinGroup(
            @RequestParam String userId,
            @RequestParam String groupId,
            HttpServletRequest request) {

        String clientId = generateClientId(request, userId);

        // 创建SSE连接
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

        // 添加到消息服务
        groupMessageService.addClientConnection(clientId, userId, emitter);

        // 加入群组
        boolean joined = groupMembersManager.joinGroup(clientId, userId, groupId);

        if (joined) {
            // 发送加入事件
            GroupEvent joinEvent = GroupEvent.join(clientId, userId, groupId);
            groupMessageService.sendMemberChangeEvent(groupId, joinEvent);

            // 发送欢迎消息
            NotificationMessage welcomeMsg = NotificationMessage.builder()
                    .type("welcome")
                    .title("欢迎加入群组")
                    .content(userId + " 加入了群组")
                    .sender("system")
                    .groupId(groupId)
                    .createdAt(LocalDateTime.now())
                    .build();

            groupMessageService.sendToGroup(groupId, welcomeMsg);

            log.info("用户加入群组成功: userId={}, groupId={}, clientId={}",
                    userId, groupId, clientId);
        }

        return emitter;
    }

    /**
     * 发送群组消息
     */
    @PostMapping("/{groupId}/send")
    public ResponseEntity<ApiResponse> sendGroupMessage(
            @PathVariable String groupId,
            @RequestParam String userId,
            @RequestBody MessageRequest messageRequest) {

        NotificationMessage message = NotificationMessage.builder()
                .id(UUID.randomUUID().toString())
                .type("chat")
                .title(messageRequest.getTitle())
                .content(messageRequest.getContent())
                .sender(userId)
                .groupId(groupId)
                .createdAt(LocalDateTime.now())
                .extra(messageRequest.getExtra())
                .build();

        groupMessageService.sendToGroup(groupId, message);

        return ResponseEntity.ok(ApiResponse.success("消息发送成功"));
    }

    /**
     * 获取群组成员
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<ApiResponse> getGroupMembers(@PathVariable String groupId) {
        Set<String> members = groupMembersManager.getGroupMembers(groupId);
        Set<String> onlineMembers = groupMessageService.getOnlineGroupMembers(groupId);

        Map<String, Object> result = Map.of(
                "totalMembers", members.size(),
                "onlineMembers", onlineMembers.size(),
                "members", members,
                "onlineMembers", onlineMembers
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 离开群组
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<ApiResponse> leaveGroup(
            @PathVariable String groupId,
            @RequestParam String userId) {

        // 找到对应的clientId（实际应用中需要根据session获取）
        String clientId = findClientIdByUserId(userId);

        if (clientId != null) {
            boolean left = groupMembersManager.leaveGroup(clientId, groupId);

            if (left) {
                // 发送离开事件
                GroupEvent leaveEvent = GroupEvent.leave(clientId, userId, groupId);
                groupMessageService.sendMemberChangeEvent(groupId, leaveEvent);

                // 移除客户端连接
                groupMessageService.removeClient(clientId);

                return ResponseEntity.ok(ApiResponse.success("离开群组成功"));
            }
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error("离开群组失败"));
    }

    /**
     * 获取群组统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse> getStatistics() {
        Map<String, Object> stats = groupMembersManager.getGroupStatistics();
        stats.put("onlineClients", groupMessageService.getOnlineClients().size());

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * 生成客户端ID
     */
    private String generateClientId(HttpServletRequest request, String userId) {
        String ip = request.getRemoteAddr();
        String sessionId = request.getSession().getId();
        return userId + "_" + ip + "_" + sessionId.hashCode();
    }

    /**
     * 根据用户ID查找客户端ID（简化版）
     */
    private String findClientIdByUserId(String userId) {
        // 实际应用中需要通过会话管理来查找
        return groupMessageService.getOnlineClients().stream()
                .filter(clientId -> clientId.startsWith(userId + "_"))
                .findFirst()
                .orElse(null);
    }
}