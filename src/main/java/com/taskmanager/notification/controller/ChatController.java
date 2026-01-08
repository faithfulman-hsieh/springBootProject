package com.taskmanager.notification.controller;

import com.taskmanager.notification.model.ChatMessage;
import com.taskmanager.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class ChatController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatController(NotificationService notificationService, SimpMessagingTemplate messagingTemplate) {
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    // ==========================================
    //  WebSocket Endpoints (STOMP)
    // ==========================================

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        notificationService.sendGlobalMessage(chatMessage);
    }

    // ★★★ 新增：處理私訊的 Endpoint ★★★
    @MessageMapping("/chat.sendPrivateMessage")
    public void sendPrivateMessage(@Payload ChatMessage chatMessage) {
        notificationService.sendPrivateMessage(chatMessage);
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage) {
        chatMessage.setContent("加入了聊天室");
        chatMessage.setType("JOIN");
        notificationService.sendGlobalMessage(chatMessage);
    }

    @MessageMapping("/chat.signal")
    public void signal(@Payload ChatMessage message) {
        if (message.getReceiver() != null && !message.getReceiver().isEmpty()) {
            messagingTemplate.convertAndSendToUser(
                    message.getReceiver(),
                    "/queue/signal",
                    message
            );
        }
    }

    // ==========================================
    //  REST Endpoints (HTTP)
    // ==========================================

    @GetMapping("/api/chat/public-history")
    public ResponseEntity<List<ChatMessage>> getPublicHistory() {
        return ResponseEntity.ok(notificationService.getPublicHistory());
    }

    @PostMapping("/api/chat/send")
    public ResponseEntity<Void> sendRestMessage(@RequestBody ChatMessage chatMessage) {
        notificationService.sendGlobalMessage(chatMessage);
        return ResponseEntity.ok().build();
    }
}