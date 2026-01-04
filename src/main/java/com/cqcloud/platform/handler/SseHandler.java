package com.cqcloud.platform.handler;

import com.cqcloud.platform.dto.ApiResponse;
import com.cqcloud.platform.dto.ConnectResponse;
import com.cqcloud.platform.dto.MessageRequest;
import com.cqcloud.platform.dto.SseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE处理
 * @author weimeilayer@gmail.com ✨
 * @date 💓💕 2024年4月12日 🐬🐇 💓💕
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
public class SseHandler  {

	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	/**
	 * 创建SSE连接
	 * @param clientId 客户端ID
	 * @return SSE发射器
	 */
	@GetMapping(value = "/connect/{clientId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter connect(@PathVariable String clientId,
							  @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) {

		log.info("SSE连接请求: clientId={}, lastEventId={}", clientId, lastEventId);

		// 设置连接超时（建议设置为30分钟到1小时）
		SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);

		// 存储连接
		emitters.put(clientId, emitter);

		try {
			// 发送连接成功事件
			SseEvent connectEvent = SseEvent.builder()
					.event("connect")
					.id(generateEventId())
					.data(new ConnectResponse("连接成功", LocalDateTime.now()))
					.retry(5000L) // 重连时间
					.build();

			emitter.send(connectEvent);

			// 如果有上次最后的事件ID，发送错过的消息
			if (lastEventId != null) {
				sendMissedMessages(clientId, lastEventId, emitter);
			}

		} catch (IOException e) {
			log.error("SSE连接初始化失败", e);
			emitters.remove(clientId);
			emitter.completeWithError(e);
			return emitter;
		}

		// 设置完成回调
		emitter.onCompletion(() -> {
			log.info("SSE连接完成: clientId={}", clientId);
			emitters.remove(clientId);
			notifyConnectionStatus(clientId, false);
		});

		// 设置超时回调
		emitter.onTimeout(() -> {
			log.warn("SSE连接超时: clientId={}", clientId);
			emitter.complete();
		});

		// 设置错误回调
		emitter.onError((ex) -> {
			log.error("SSE连接错误: clientId={}", clientId, ex);
			emitters.remove(clientId);
		});

		// 通知连接状态
		notifyConnectionStatus(clientId, true);

		return emitter;
	}

	/**
	 * 发送消息给指定客户端
	 */
	@PostMapping("/send/{clientId}")
	public ResponseEntity<ApiResponse> sendMessage(@PathVariable String clientId,
												   @RequestBody MessageRequest request) {
		try {
			SseEmitter emitter = emitters.get(clientId);
			if (emitter == null) {
				return ResponseEntity.status(404)
						.body(ApiResponse.error("客户端未连接"));
			}

			SseEvent event = SseEvent.builder()
					.event("message")
					.id(generateEventId())
					.data(request.getContent())
					.build();

			emitter.send(event);

			// 记录发送成功
			recordMessageSent(clientId, "message");

			return ResponseEntity.ok(ApiResponse.success("消息发送成功"));
		} catch (IOException e) {
			log.error("发送消息失败", e);
			emitters.remove(clientId);
			return ResponseEntity.status(500)
					.body(ApiResponse.error("发送消息失败"));
		}
	}

	/**
	 * 广播消息给所有客户端
	 */
	@PostMapping("/broadcast")
	public ResponseEntity<ApiResponse> broadcast(@RequestBody MessageRequest request) {
		int successCount = 0;
		int failCount = 0;

		for (Map.Entry<String, SseEmitter> entry : emitters.entrySet()) {
			try {
				SseEvent event = SseEvent.builder()
						.event("broadcast")
						.id(generateEventId())
						.data(request.getContent())
						.build();

				entry.getValue().send(event);
				successCount++;
			} catch (IOException e) {
				log.error("广播消息失败: clientId={}", entry.getKey(), e);
				emitters.remove(entry.getKey());
				failCount++;
			}
		}

		return ResponseEntity.ok(ApiResponse.success(
				String.format("广播完成，成功: %d, 失败: %d", successCount, failCount)
		));
	}

	/**
	 * 获取活跃连接列表
	 */
	@GetMapping("/connections")
	public ResponseEntity<ApiResponse> getActiveConnections() {
		return ResponseEntity.ok(ApiResponse.success(
				emitters.keySet().stream().toList()
		));
	}

	/**
	 * 关闭指定连接
	 */
	@DeleteMapping("/disconnect/{clientId}")
	public ResponseEntity<ApiResponse> disconnect(@PathVariable String clientId) {
		SseEmitter emitter = emitters.remove(clientId);
		if (emitter != null) {
			emitter.complete();
			return ResponseEntity.ok(ApiResponse.success("连接已关闭"));
		}
		return ResponseEntity.status(404)
				.body(ApiResponse.error("连接不存在"));
	}

	private void sendMissedMessages(String clientId, String lastEventId, SseEmitter emitter) {
		// 从数据库或缓存中获取错过的消息
		//List<Message> missedMessages = messageService.getMessagesAfter(lastEventId);
		// 发送给客户端
	}

	private void notifyConnectionStatus(String clientId, boolean connected) {
		// 通知其他服务连接状态变化
	}

	private void recordMessageSent(String clientId, String messageType) {
		// 记录消息发送指标
	}

	private String generateEventId() {
		return String.valueOf(System.currentTimeMillis());
	}
}