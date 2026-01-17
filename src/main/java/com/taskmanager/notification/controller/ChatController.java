package com.taskmanager.notification.controller;

import com.taskmanager.notification.dto.ChatMessage;
import com.taskmanager.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser; // ★★★ [線上使用者狀態] ★★★
import org.springframework.messaging.simp.user.SimpUserRegistry; // ★★★ [線上使用者狀態] ★★★
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set; // ★★★ [線上使用者狀態] ★★★
import java.util.stream.Collectors; // ★★★ [線上使用者狀態] ★★★

@RestController
public class ChatController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    // ★★★ [線上使用者狀態] 注入 Registry 以獲取即時連線名單 ★★★
    @Autowired
    private SimpUserRegistry simpUserRegistry;

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

    // ★★★ 新增：獲取私訊歷史 ★★★
    @GetMapping("/api/chat/history/{contact}")
    public ResponseEntity<List<ChatMessage>> getPrivateHistory(
            @PathVariable String contact,
            Authentication authentication) {
        String currentUser = authentication.getName();
        return ResponseEntity.ok(notificationService.getPrivateHistory(currentUser, contact));
    }

    // ★★★ 新增：獲取未讀數量 ★★★
    @GetMapping("/api/chat/unread/{contact}")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable String contact,
            Authentication authentication) {
        String currentUser = authentication.getName();
        return ResponseEntity.ok(notificationService.getUnreadCount(contact, currentUser));
    }

    // ★★★ 新增：標記已讀 ★★★
    @PostMapping("/api/chat/read/{contact}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String contact,
            Authentication authentication) {
        String currentUser = authentication.getName();
        notificationService.markAsRead(contact, currentUser);
        return ResponseEntity.ok().build();
    }

    // ★★★ [線上使用者狀態] 新增 API：前端初始化時拉取線上名單 ★★★
    @GetMapping("/api/chat/online-users")
    public ResponseEntity<Set<String>> getOnlineUsers() {
        Set<String> onlineUsers = simpUserRegistry.getUsers().stream()
                .map(SimpUser::getName)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(onlineUsers);
    }

    @PostMapping("/api/chat/send")
    public ResponseEntity<Void> sendRestMessage(@RequestBody ChatMessage chatMessage) {
        notificationService.sendGlobalMessage(chatMessage);
        return ResponseEntity.ok().build();
    }
}