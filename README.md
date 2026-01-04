客户端JavaScript示例


javascript
class SSEClient {
constructor(options = {}) {
this.clientId = options.clientId || this.generateClientId();
this.userId = options.userId;
this.baseUrl = options.baseUrl || '/api/sse';
this.eventSource = null;
this.reconnectDelay = options.reconnectDelay || 3000;
this.maxReconnectAttempts = options.maxReconnectAttempts || 5;
this.reconnectAttempts = 0;
this.lastEventId = null;
this.eventHandlers = new Map();

        // 默认事件处理器
        this.on('connect', this.handleConnect.bind(this));
        this.on('heartbeat', this.handleHeartbeat.bind(this));
        this.on('error', this.handleError.bind(this));
    }
    
    /**
     * 连接服务器
     */
    connect() {
        if (this.eventSource) {
            this.disconnect();
        }
        
        let url = `${this.baseUrl}/connect/${this.clientId}`;
        if (this.userId) {
            url += `?userId=${this.userId}`;
        }
        if (this.lastEventId) {
            url += `&lastEventId=${this.lastEventId}`;
        }
        
        this.eventSource = new EventSource(url);
        
        // 监听消息事件
        this.eventSource.onmessage = (event) => {
            this.handleEvent(event);
        };
        
        // 监听错误事件
        this.eventSource.onerror = (error) => {
            console.error('SSE连接错误:', error);
            this.handleDisconnect();
        };
        
        // 监听特定事件
        this.eventSource.addEventListener('connect', (event) => {
            const data = JSON.parse(event.data);
            this.emit('connect', data);
        });
        
        this.eventSource.addEventListener('heartbeat', (event) => {
            this.emit('heartbeat', event.data);
        });
    }
    
    /**
     * 断开连接
     */
    disconnect() {
        if (this.eventSource) {
            this.eventSource.close();
            this.eventSource = null;
        }
    }
    
    /**
     * 发送消息
     */
    sendMessage(content, type = 'message') {
        return fetch(`${this.baseUrl}/send/${this.clientId}`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                content: content,
                type: type,
                timestamp: new Date().toISOString()
            })
        });
    }
    
    /**
     * 加入群组
     */
    joinGroup(groupId) {
        this.sendMessage(groupId, 'join_group');
    }
    
    /**
     * 离开群组
     */
    leaveGroup(groupId) {
        this.sendMessage(groupId, 'leave_group');
    }
    
    /**
     * 事件处理
     */
    on(eventName, handler) {
        if (!this.eventHandlers.has(eventName)) {
            this.eventHandlers.set(eventName, []);
        }
        this.eventHandlers.get(eventName).push(handler);
    }
    
    /**
     * 触发事件
     */
    emit(eventName, data) {
        const handlers = this.eventHandlers.get(eventName);
        if (handlers) {
            handlers.forEach(handler => {
                try {
                    handler(data);
                } catch (error) {
                    console.error(`事件处理器错误: ${eventName}`, error);
                }
            });
        }
    }
    
    /**
     * 处理连接事件
     */
    handleConnect(data) {
        console.log('SSE连接成功:', data);
        this.reconnectAttempts = 0; // 重置重连次数
        this.lastEventId = data.lastEventId;
    }
    
    /**
     * 处理心跳
     */
    handleHeartbeat(data) {
        // 更新最后活动时间
        this.lastActivity = Date.now();
    }
    
    /**
     * 处理错误
     */
    handleError(error) {
        console.error('SSE错误:', error);
    }
    
    /**
     * 处理断开连接
     */
    handleDisconnect() {
        this.disconnect();
        
        if (this.reconnectAttempts < this.maxReconnectAttempts) {
            this.reconnectAttempts++;
            console.log(`尝试重连 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
            
            setTimeout(() => {
                this.connect();
            }, this.reconnectDelay * this.reconnectAttempts); // 指数退避
        } else {
            console.error('达到最大重连次数，停止重连');
            this.emit('disconnect', { reason: 'max_reconnect_attempts' });
        }
    }
    
    /**
     * 处理通用事件
     */
    handleEvent(event) {
        try {
            const eventName = event.type || 'message';
            let eventData;
            
            if (event.data) {
                eventData = JSON.parse(event.data);
            }
            
            // 更新最后事件ID
            if (event.lastEventId) {
                this.lastEventId = event.lastEventId;
            }
            
            this.emit(eventName, eventData);
            
        } catch (error) {
            console.error('处理事件失败:', error);
        }
    }
    
    /**
     * 生成客户端ID
     */
    generateClientId() {
        return 'client_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
    }
}

// 使用示例
const sseClient = new SSEClient({
userId: 'user123',
baseUrl: 'http://localhost:8080/api/sse'
});

// 监听事件
sseClient.on('connect', (data) => {
console.log('连接成功:', data);
});

sseClient.on('message', (message) => {
console.log('收到消息:', message);
showNotification(message.content);
});

sseClient.on('order_update', (order) => {
updateOrderStatus(order);
});

sseClient.on('disconnect', (reason) => {
console.log('连接断开:', reason);
});

// 连接服务器
sseClient.connect();

// 发送消息
sseClient.sendMessage('Hello, Server!');

// 加入群组
sseClient.joinGroup('order_group');