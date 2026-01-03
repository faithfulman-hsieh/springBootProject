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

    // ★★★ 新增：暫存聊天記錄 (重啟後會消失，若需持久化可改用 DB) ★★★
    private final List<ChatMessage> history = new CopyOnWriteArrayList<>();

    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendGlobalMessage(ChatMessage message) {
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        // 儲存到歷史紀錄 (限制最近 50 筆以免佔用過多記憶體)
        if (history.size() >= 50) {
            history.remove(0);
        }
        history.add(message);

        messagingTemplate.convertAndSend("/topic/public-chat", message);
    }

    public void sendNotificationToUser(String username, String content) {
        ChatMessage message = new ChatMessage();
        message.setSender("System");
        message.setContent(content);
        message.setType("NOTIFICATION");
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        messagingTemplate.convertAndSendToUser(username, "/queue/notifications", message);
    }

    // ★★★ 新增：取得歷史紀錄 ★★★
    public List<ChatMessage> getPublicHistory() {
        return new ArrayList<>(history);
    }
}