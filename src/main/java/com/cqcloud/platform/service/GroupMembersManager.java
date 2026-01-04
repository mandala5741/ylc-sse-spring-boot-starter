package com.cqcloud.platform.service;


import com.cqcloud.platform.dto.GroupInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 群组成员管理服务
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Slf4j
@Service
public class GroupMembersManager {

    // 存储群组成员：groupId -> Set<clientId>
    private final Map<String, Set<String>> groupMembers = new ConcurrentHashMap<>();

    // 存储客户端所属群组：clientId -> Set<groupId>
    private final Map<String, Set<String>> clientGroups = new ConcurrentHashMap<>();

    // 存储群组信息：groupId -> GroupInfo
    private final Map<String, GroupInfo> groupInfos = new ConcurrentHashMap<>();



    /**
     * 加入群组
     */
    public synchronized boolean joinGroup(String clientId, String userId, String groupId) {
        if (!isGroupExists(groupId)) {
            // 如果群组不存在，自动创建
            createGroup(groupId, userId, "自动创建群组");
        }

        // 添加到群组成员
        Set<String> members = groupMembers.computeIfAbsent(groupId,
                k -> ConcurrentHashMap.newKeySet());

        if (members.add(clientId)) {
            // 添加到客户端群组列表
            clientGroups.computeIfAbsent(clientId,
                            k -> ConcurrentHashMap.newKeySet())
                    .add(groupId);

            log.info("用户加入群组: userId={}, clientId={}, groupId={}",
                    userId, clientId, groupId);
            return true;
        }

        return false;
    }

    /**
     * 离开群组
     */
    public synchronized boolean leaveGroup(String clientId, String groupId) {
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            boolean removed = members.remove(clientId);

            if (removed) {
                // 从客户端群组列表中移除
                Set<String> groups = clientGroups.get(clientId);
                if (groups != null) {
                    groups.remove(groupId);
                }

                // 如果群组为空，清理群组
                if (members.isEmpty()) {
                    groupMembers.remove(groupId);
                    groupInfos.remove(groupId);
                }

                log.info("用户离开群组: clientId={}, groupId={}", clientId, groupId);
                return true;
            }
        }
        return false;
    }

    /**
     * 踢出成员
     */
    public synchronized boolean kickFromGroup(String clientId, String groupId, String operator) {
        // 检查操作者权限
        if (!isGroupAdmin(groupId, operator)) {
            log.warn("无权限踢出成员: operator={}, groupId={}", operator, groupId);
            return false;
        }

        return leaveGroup(clientId, groupId);
    }

    /**
     * 获取群组成员
     */
    public Set<String> getGroupMembers(String groupId) {
        Set<String> members = groupMembers.get(groupId);
        return members != null ? new HashSet<>(members) : Collections.emptySet();
    }

    /**
     * 获取客户端加入的群组
     */
    public Set<String> getClientGroups(String clientId) {
        Set<String> groups = clientGroups.get(clientId);
        return groups != null ? new HashSet<>(groups) : Collections.emptySet();
    }

    /**
     * 获取群组成员数量
     */
    public int getGroupMemberCount(String groupId) {
        Set<String> members = groupMembers.get(groupId);
        return members != null ? members.size() : 0;
    }

    /**
     * 判断用户是否在群组中
     */
    public boolean isMemberInGroup(String clientId, String groupId) {
        Set<String> members = groupMembers.get(groupId);
        return members != null && members.contains(clientId);
    }

    /**
     * 创建群组
     */
    public synchronized void createGroup(String groupId, String creator, String groupName) {
        GroupInfo groupInfo = GroupInfo.builder()
                .groupId(groupId)
                .groupName(groupName)
                .creator(creator)
                .createTime(new Date())
                .administrators(new HashSet<>(Arrays.asList(creator)))
                .metadata(new HashMap<>())
                .build();

        groupInfos.put(groupId, groupInfo);

        // 初始化成员集合
        groupMembers.put(groupId, ConcurrentHashMap.newKeySet());

        log.info("创建群组: groupId={}, creator={}", groupId, creator);
    }

    /**
     * 获取群组信息
     */
    public GroupInfo getGroupInfo(String groupId) {
        return groupInfos.get(groupId);
    }

    /**
     * 获取所有群组
     */
    public List<String> getAllGroups() {
        return new ArrayList<>(groupMembers.keySet());
    }

    /**
     * 解散群组
     */
    public synchronized boolean disbandGroup(String groupId, String operator) {
        GroupInfo groupInfo = groupInfos.get(groupId);
        if (groupInfo == null) {
            return false;
        }

        // 检查权限：只有创建者或管理员可以解散
        if (!groupInfo.getCreator().equals(operator) &&
                !groupInfo.getAdministrators().contains(operator)) {
            log.warn("无权限解散群组: operator={}, groupId={}", operator, groupId);
            return false;
        }

        // 清理所有成员的群组记录
        Set<String> members = groupMembers.get(groupId);
        if (members != null) {
            members.forEach(clientId -> {
                Set<String> groups = clientGroups.get(clientId);
                if (groups != null) {
                    groups.remove(groupId);
                }
            });
        }

        // 删除群组
        groupMembers.remove(groupId);
        groupInfos.remove(groupId);

        log.info("解散群组: groupId={}, operator={}", groupId, operator);
        return true;
    }

    /**
     * 添加管理员
     */
    public synchronized boolean addAdministrator(String groupId, String userId, String operator) {
        GroupInfo groupInfo = groupInfos.get(groupId);
        if (groupInfo == null) {
            return false;
        }

        // 检查操作者权限
        if (!groupInfo.getAdministrators().contains(operator)) {
            log.warn("无权限添加管理员: operator={}", operator);
            return false;
        }

        groupInfo.getAdministrators().add(userId);
        return true;
    }

    /**
     * 群组是否存在
     */
    public boolean isGroupExists(String groupId) {
        return groupMembers.containsKey(groupId);
    }

    /**
     * 是否是群组管理员
     */
    public boolean isGroupAdmin(String groupId, String userId) {
        GroupInfo groupInfo = groupInfos.get(groupId);
        return groupInfo != null && groupInfo.getAdministrators().contains(userId);
    }

    /**
     * 获取群组统计信息
     */
    public Map<String, Object> getGroupStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalGroups", groupMembers.size());

        int totalMembers = groupMembers.values().stream()
                .mapToInt(Set::size)
                .sum();
        stats.put("totalMembers", totalMembers);

        Map<String, Integer> groupSizes = groupMembers.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
        stats.put("groupSizes", groupSizes);

        return stats;
    }

    /**
     * 清理无效连接
     */
    public synchronized void cleanupInvalidClients(Set<String> validClients) {
        // 清理客户端群组映射
        clientGroups.entrySet().removeIf(entry -> !validClients.contains(entry.getKey()));

        // 清理群组成员
        groupMembers.forEach((groupId, members) -> {
            members.removeIf(clientId -> !validClients.contains(clientId));

            // 如果群组为空，删除群组
            if (members.isEmpty()) {
                groupMembers.remove(groupId);
                groupInfos.remove(groupId);
            }
        });
    }
}