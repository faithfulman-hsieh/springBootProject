package com.taskmanager.notification.service;

import com.taskmanager.notification.model.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // 暫存聊天記錄
    private final List<ChatMessage> history = new CopyOnWriteArrayList<>();

    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendGlobalMessage(ChatMessage message) {
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        if (history.size() >= 50) {
            history.remove(0);
        }
        history.add(message);

        messagingTemplate.convertAndSend("/topic/public-chat", message);
    }

    // ★★★ 新增：發送私訊的方法 ★★★
    public void sendPrivateMessage(ChatMessage message) {
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        // 1. 發送給接收者 (Receiver)
        // 對應前端訂閱的路徑: /user/queue/messages
        messagingTemplate.convertAndSendToUser(
                message.getReceiver(),
                "/queue/messages",
                message
        );

        // 2. 同時發送給發送者 (Sender) - 讓自己的介面也能同步顯示這條訊息
        messagingTemplate.convertAndSendToUser(
                message.getSender(),
                "/queue/messages",
                message
        );

        // 注意：為了隱私，這裡暫時不將私訊存入全域 history
    }

    public void sendNotificationToUser(String username, String content) {
        ChatMessage message = new ChatMessage();
        message.setSender("System");
        message.setContent(content);
        message.setType("NOTIFICATION");
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
    }

    public List<ChatMessage> getPublicHistory() {
        return new ArrayList<>(history);
    }
}