package com.taskmanager.notification.controller;

import com.taskmanager.notification.model.ChatMessage;
import com.taskmanager.notification.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class ChatController {

    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public ChatController(NotificationService notificationService, SimpMessagingTemplate messagingTemplate) {
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    // 一般聊天 (廣播)
    // 前端發送位置: /app/chat.sendMessage
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        notificationService.sendGlobalMessage(chatMessage);
    }

    // 使用者加入
    // 前端發送位置: /app/chat.addUser
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage) {
        chatMessage.setContent("加入了聊天室");
        chatMessage.setType("JOIN");
        notificationService.sendGlobalMessage(chatMessage);
    }

    // ★★★ WebRTC 信令轉發 (點對點) ★★★
    // 前端發送至: /app/chat.signal
    @MessageMapping("/chat.signal")
    public void signal(@Payload ChatMessage message) {
        // 如果有指定接收者，就只傳給他 (例如：撥號給特定人)
        if (message.getReceiver() != null && !message.getReceiver().isEmpty()) {
            // 轉發到接收者的個人頻道: /user/{username}/queue/signal
            messagingTemplate.convertAndSendToUser(
                    message.getReceiver(),
                    "/queue/signal",
                    message
            );
        }
    }
}