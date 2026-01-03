package com.taskmanager.notification.service;

import com.taskmanager.notification.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // 1. 發送廣播訊息 (聊天室用)
    public void sendGlobalMessage(ChatMessage message) {
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));
        messagingTemplate.convertAndSend("/topic/public-chat", message);
    }

    // 2. 發送個人通知 (工作流整合用)
    // 當任務指派給某人時，呼叫此方法
    public void sendNotificationToUser(String username, String content) {
        ChatMessage message = new ChatMessage();
        message.setSender("System");
        message.setContent(content);
        message.setType("NOTIFICATION");
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        // 訊息會發送到: /user/{username}/queue/notifications
        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
    }
}