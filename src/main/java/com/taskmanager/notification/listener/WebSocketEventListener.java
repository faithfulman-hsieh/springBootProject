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
            // ★★★ [修正重複加入訊息] ★★★
            // 只記錄 Log，不在此處廣播 JOIN。
            // 因為前端會在連線後主動發送 /app/chat.addUser，由 Controller 負責廣播 JOIN。
            // 如果這裡也廣播，就會導致「XXX 加入了聊天室」出現兩次。
            logger.info("監測到 WebSocket 連線建立: {}", username);
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