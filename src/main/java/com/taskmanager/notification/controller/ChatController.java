package com.taskmanager.notification.controller;

import com.taskmanager.notification.model.ChatMessage;
import com.taskmanager.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller; // 注意：這裡可以是 Controller 或 RestController，混合使用時要注意 ResponseBody
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 改為 RestController 以支援 REST API
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

    // ★★★ 新增：獲取歷史訊息 API ★★★
    @GetMapping("/api/chat/public-history")
    public ResponseEntity<List<ChatMessage>> getPublicHistory() {
        return ResponseEntity.ok(notificationService.getPublicHistory());
    }

    // ★★★ 新增：REST 發送訊息 API (可供 Postman 測試或前端使用) ★★★
    @PostMapping("/api/chat/send")
    public ResponseEntity<Void> sendRestMessage(@RequestBody ChatMessage chatMessage) {
        notificationService.sendGlobalMessage(chatMessage);
        return ResponseEntity.ok().build();
    }
}