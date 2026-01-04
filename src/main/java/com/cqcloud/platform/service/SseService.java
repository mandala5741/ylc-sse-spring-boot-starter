package com.cqcloud.platform.service;

import com.cqcloud.platform.dto.ClientInfo;
import com.cqcloud.platform.dto.GroupEvent;
import com.cqcloud.platform.dto.SseEvent;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * SSE服务
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseService {

    private final Map<String, SseEmitter> clientEmitters = new ConcurrentHashMap<>();
    private final Map<String, ClientInfo> clientInfos = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();

    // 线程池处理异步发送
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(10);
    private final ScheduledExecutorService heartbeatExecutor =
            Executors.newSingleThreadScheduledExecutor();

    @PostConstruct
    public void init() {
        // 启动心跳检测
        heartbeatExecutor.scheduleAtFixedRate(this::checkHeartbeat,
                60, 60, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void destroy() {
        asyncExecutor.shutdown();
        heartbeatExecutor.shutdown();

        // 关闭所有连接
        clientEmitters.values().forEach(SseEmitter::complete);
        clientEmitters.clear();
        clientInfos.clear();
    }

    /**
     * 创建SSE连接
     */
    public SseEmitter createConnection(String clientId, String userId,
                                       String sessionId, String userAgent) {

        // 移除旧的连接（如果有）
        SseEmitter oldEmitter = clientEmitters.remove(clientId);
        if (oldEmitter != null) {
            oldEmitter.complete();
        }

        // 创建新的发射器
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        clientEmitters.put(clientId, emitter);

        // 记录客户端信息
        ClientInfo clientInfo = ClientInfo.builder()
                .clientId(clientId)
                .userId(userId)
                .sessionId(sessionId)
                .userAgent(userAgent)
                .connectTime(LocalDateTime.now())
                .lastHeartbeat(LocalDateTime.now())
                .build();
        clientInfos.put(clientId, clientInfo);

        // 设置回调
        emitter.onCompletion(() -> {
            log.info("连接完成: {}", clientId);
            removeConnection(clientId);
        });

        emitter.onTimeout(() -> {
            log.warn("连接超时: {}", clientId);
            sendHeartbeat(clientId); // 尝试发送心跳
        });

        emitter.onError(ex -> {
            log.error("连接错误: {}", clientId, ex);
            removeConnection(clientId);
        });

        log.info("SSE连接建立: {}", clientInfo);
        return emitter;
    }

    /**
     * 发送消息给指定客户端
     */
    public boolean sendToClient(String clientId, SseEvent event) {
        SseEmitter emitter = clientEmitters.get(clientId);
        if (emitter == null) {
            log.warn("客户端未连接: {}", clientId);
            return false;
        }

        asyncExecutor.submit(() -> {
            try {
                emitter.send(event);
                updateClientActivity(clientId);
                log.debug("消息发送成功: clientId={}, event={}", clientId, event.getEvent());
            } catch (IOException e) {
                log.error("发送消息失败: clientId={}", clientId, e);
                removeConnection(clientId);
            }
        });

        return true;
    }

    /**
     * 发送消息给用户（支持多设备）
     */
    public void sendToUser(String userId, SseEvent event) {
        clientInfos.values().stream()
                .filter(info -> userId.equals(info.getUserId()))
                .map(ClientInfo::getClientId)
                .forEach(clientId -> sendToClient(clientId, event));
    }

    /**
     * 发送消息给群组
     */
    public void sendToGroup(String groupId, SseEvent event) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.forEach(clientId -> sendToClient(clientId, event));
        }
    }

    /**
     * 加入群组
     */
    public void joinGroup(String clientId, String groupId) {
        groupMembers.computeIfAbsent(groupId, k -> ConcurrentHashMap.newKeySet())
                .add(clientId);

        // 通知群组成员
        SseEvent event = SseEvent.builder()
                .event("group_join")
                .data(new GroupEvent(clientId, groupId, "join"))
                .build();

        sendToGroup(groupId, event);
    }

    /**
     * 离开群组
     */
    public void leaveGroup(String clientId, String groupId) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.remove(clientId);

            // 通知群组成员
            SseEvent event = SseEvent.builder()
                    .event("group_leave")
                    .data(new GroupEvent(clientId, groupId, "leave"))
                    .build();

            sendToGroup(groupId, event);
        }
    }

    /**
     * 发送心跳
     */
    private void sendHeartbeat(String clientId) {
        SseEvent heartbeat = SseEvent.builder()
                .event("heartbeat")
                .data(LocalDateTime.now().toString())
                .build();

        if (!sendToClient(clientId, heartbeat)) {
            log.info("心跳检测失败，移除连接: {}", clientId);
            removeConnection(clientId);
        }
    }

    /**
     * 定期心跳检测
     */
    private void checkHeartbeat() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(2);

        clientInfos.entrySet().removeIf(entry -> {
            if (entry.getValue().getLastHeartbeat().isBefore(threshold)) {
                log.info("心跳超时，移除连接: {}", entry.getKey());
                SseEmitter emitter = clientEmitters.remove(entry.getKey());
                if (emitter != null) {
                    emitter.complete();
                }
                return true;
            }
            return false;
        });
    }

    /**
     * 更新客户端活动时间
     */
    private void updateClientActivity(String clientId) {
        ClientInfo info = clientInfos.get(clientId);
        if (info != null) {
            info.setLastHeartbeat(LocalDateTime.now());
        }
    }

    /**
     * 移除连接
     */
    private void removeConnection(String clientId) {
        SseEmitter emitter = clientEmitters.remove(clientId);
        if (emitter != null) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // 忽略完成异常
            }
        }

        clientInfos.remove(clientId);

        // 从所有群组中移除
        groupMembers.values().forEach(members -> members.remove(clientId));

        log.info("连接移除: {}", clientId);
    }

    /**
     * 获取活跃连接数
     */
    public int getActiveConnectionCount() {
        return clientEmitters.size();
    }

    /**
     * 获取客户端信息
     */
    public List<ClientInfo> getActiveClients() {
        return new ArrayList<>(clientInfos.values());
    }
}