package com.taskmanager.notification.service;

import com.taskmanager.notification.dto.ChatMessage;
import com.taskmanager.notification.model.ChatMessageEntity;
import com.taskmanager.notification.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatRepository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // 暫存廣播聊天記錄 (原有功能保持不變)
    private final List<ChatMessage> history = new CopyOnWriteArrayList<>();

    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate, ChatMessageRepository chatRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatRepository = chatRepository;
    }

    // 發送廣播訊息
    public void sendGlobalMessage(ChatMessage message) {
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        if (history.size() >= 50) {
            history.remove(0);
        }
        history.add(message);

        messagingTemplate.convertAndSend("/topic/public-chat", message);
    }

    // ★★★ 修改：發送私訊 (整合資料庫儲存) ★★★
    @Transactional
    public void sendPrivateMessage(ChatMessage message) {
        // 1. 存入資料庫
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSender(message.getSender());
        entity.setReceiver(message.getReceiver());
        entity.setContent(message.getContent());
        entity.setType(message.getType());
        entity.setSendTime(LocalDateTime.now());
        entity.setRead(false);
        chatRepository.save(entity);

        // 2. 設定顯示時間
        message.setTime(LocalDateTime.now().format(TIME_FORMATTER));

        // 3. 發送給接收者
        messagingTemplate.convertAndSendToUser(
                message.getReceiver(),
                "/queue/messages",
                message
        );

        // 4. 同時發送給發送者 (讓自己的前端也能即時顯示)
        messagingTemplate.convertAndSendToUser(
                message.getSender(),
                "/queue/messages",
                message
        );
    }

    // ★★★ 新增：獲取私人歷史紀錄 ★★★
    public List<ChatMessage> getPrivateHistory(String user1, String user2) {
        List<ChatMessageEntity> entities = chatRepository.findChatHistory(user1, user2);
        return entities.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    // ★★★ 新增：標記已讀 ★★★
    @Transactional
    public void markAsRead(String sender, String receiver) {
        chatRepository.markMessagesAsRead(sender, receiver);
    }

    // ★★★ 新增：獲取未讀數量 ★★★
    public long getUnreadCount(String sender, String receiver) {
        return chatRepository.countBySenderAndReceiverAndIsReadFalse(sender, receiver);
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

    // Entity 轉 DTO 工具方法
    private ChatMessage convertToDto(ChatMessageEntity entity) {
        ChatMessage dto = new ChatMessage();
        dto.setSender(entity.getSender());
        dto.setReceiver(entity.getReceiver());
        dto.setContent(entity.getContent());
        dto.setType(entity.getType());
        dto.setTime(entity.getSendTime().format(TIME_FORMATTER));
        return dto;
    }
}