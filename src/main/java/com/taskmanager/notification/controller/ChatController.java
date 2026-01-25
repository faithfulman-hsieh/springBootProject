package com.taskmanager.notification.controller;

import com.taskmanager.notification.dto.ChatMessage;
import com.taskmanager.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
public class ChatController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SimpUserRegistry simpUserRegistry;

    @Autowired
    public ChatController(NotificationService notificationService, SimpMessagingTemplate messagingTemplate) {
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

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

    @MessageMapping("/chat.typing")
    public void typing(@Payload ChatMessage chatMessage) {
        chatMessage.setType("TYPING");
        if (chatMessage.getReceiver() != null && !chatMessage.getReceiver().isEmpty()) {
            messagingTemplate.convertAndSendToUser(
                    chatMessage.getReceiver(),
                    "/queue/messages",
                    chatMessage
            );
        } else {
            messagingTemplate.convertAndSend("/topic/public-chat", chatMessage);
        }
    }

    // ★★★ [Push] 修改：WebRTC 信令處理 ★★★
    @MessageMapping("/chat.signal")
    public void signal(@Payload ChatMessage message) {
        if (message.getReceiver() != null && !message.getReceiver().isEmpty()) {

            // 1. 優先透過 WebSocket 即時轉發
            messagingTemplate.convertAndSendToUser(
                    message.getReceiver(),
                    "/queue/signal",
                    message
            );

            // 2. 如果是關鍵信令 (來電 OFFER / 掛斷 HANGUP)，觸發 FCM 推播
            // 這樣即使對方在背景或手機休眠，也能收到震動通知
            if ("OFFER".equals(message.getType()) || "HANGUP".equals(message.getType())) {
                notificationService.sendPushNotification(message);
            }
        }
    }

    @GetMapping("/api/chat/public-history")
    public ResponseEntity<List<ChatMessage>> getPublicHistory() {
        return ResponseEntity.ok(notificationService.getPublicHistory());
    }

    @GetMapping("/api/chat/history/{contact}")
    public ResponseEntity<List<ChatMessage>> getPrivateHistory(
            @PathVariable String contact,
            Authentication authentication) {
        String currentUser = authentication.getName();
        return ResponseEntity.ok(notificationService.getPrivateHistory(currentUser, contact));
    }

    @GetMapping("/api/chat/unread/{contact}")
    public ResponseEntity<Long> getUnreadCount(
            @PathVariable String contact,
            Authentication authentication) {
        String currentUser = authentication.getName();
        return ResponseEntity.ok(notificationService.getUnreadCount(contact, currentUser));
    }

    @PostMapping("/api/chat/read/{contact}")
    public ResponseEntity<Void> markAsRead(
            @PathVariable String contact,
            Authentication authentication) {
        String currentUser = authentication.getName();
        notificationService.markAsRead(contact, currentUser);

        ChatMessage readReceipt = new ChatMessage();
        readReceipt.setType("READ");
        readReceipt.setSender(currentUser);
        readReceipt.setReceiver(contact);

        messagingTemplate.convertAndSendToUser(
                contact,
                "/queue/messages",
                readReceipt
        );

        return ResponseEntity.ok().build();
    }

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