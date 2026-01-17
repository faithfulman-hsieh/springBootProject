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

// ★★★ [線上使用者狀態] 新增監聽器，負責廣播 JOIN/LEAVE ★★★
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
            logger.info("使用者上線: {}", username);

            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setType("JOIN");
            chatMessage.setSender(username);
            chatMessage.setContent("加入了聊天室");

            messagingTemplate.convertAndSend("/topic/public-chat", chatMessage);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        if (headerAccessor.getUser() != null) {
            String username = headerAccessor.getUser().getName();

            // 檢查該使用者是否還有其他連線 (避免多視窗關閉其一就顯示下線)
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
        }
    }
}