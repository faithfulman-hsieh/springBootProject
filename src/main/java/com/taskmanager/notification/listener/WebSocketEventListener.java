package com.taskmanager.notification.listener;

import com.taskmanager.notification.dto.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpMessageSendingOperations messagingTemplate;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    public WebSocketEventListener(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();
            logger.info("監測到 WebSocket 連線建立: {}", username);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();

            // ★★★ [Line-like Mechanism] 修改：僅記錄 Log，不廣播 LEAVE ★★★
            // 這樣前端就不會收到「對方已離開」的訊號，保持「永遠可聯繫」的狀態。

            logger.info("使用者斷線 (背景或關閉): {}", username);

            /* 原本的離線廣播邏輯已註解掉
            boolean isStillConnected = simpUserRegistry.getUser(username) != null &&
                    !simpUserRegistry.getUser(username).getSessions().isEmpty();

            if (!isStillConnected) {
                logger.info("使用者完全離線: {}", username);
                ChatMessage chatMessage = new ChatMessage();
                chatMessage.setType("LEAVE");
                chatMessage.setSender(username);
                chatMessage.setContent("離開聊天室");

                messagingTemplate.convertAndSend("/topic/public-chat", chatMessage);
            }
            */
        }
    }
}