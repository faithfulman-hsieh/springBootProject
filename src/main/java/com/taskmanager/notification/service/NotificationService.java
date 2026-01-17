package com.taskmanager.notification.service;

import com.taskmanager.notification.dto.ChatMessage;
import com.taskmanager.notification.model.ChatMessageEntity;
import com.taskmanager.notification.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    private final ChatMessageRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    @Autowired
    public NotificationService(ChatMessageRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    public void sendGlobalMessage(ChatMessage message) {
        // ★★★ [Fix] 修正方法名稱：DTO 使用 setTime ★★★
        message.setTime(LocalDateTime.now().toString());
        saveMessage(message);
        messagingTemplate.convertAndSend("/topic/public-chat", message);
    }

    public void sendPrivateMessage(ChatMessage message) {
        // ★★★ [Fix] 修正方法名稱：DTO 使用 setTime ★★★
        message.setTime(LocalDateTime.now().toString());
        saveMessage(message);
        messagingTemplate.convertAndSendToUser(message.getReceiver(), "/queue/messages", message);

        // 同步發送給寄件者自己
        messagingTemplate.convertAndSendToUser(message.getSender(), "/queue/messages", message);
    }

    public List<ChatMessage> getPublicHistory() {
        return repository.findByReceiverIsNullOrderByTimestampAsc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public List<ChatMessage> getPrivateHistory(String user1, String user2) {
        return repository.findPrivateMessages(user1, user2).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // ★★★ [即時已讀回執] 實作未讀計數 ★★★
    public long getUnreadCount(String sender, String receiver) {
        return repository.countBySenderAndReceiverAndIsReadFalse(sender, receiver);
    }

    // ★★★ [即時已讀回執] 實作標記已讀 ★★★
    public void markAsRead(String sender, String receiver) {
        repository.markMessagesAsRead(sender, receiver);
    }

    private void saveMessage(ChatMessage dto) {
        if ("TYPING".equals(dto.getType()) || "READ".equals(dto.getType())) {
            return;
        }

        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setSender(dto.getSender());
        entity.setReceiver(dto.getReceiver());
        entity.setContent(dto.getContent());
        entity.setType(dto.getType());
        // Entity 使用 setTimestamp
        entity.setTimestamp(LocalDateTime.now());
        // ★★★ [即時已讀回執] 預設為未讀 ★★★
        entity.setRead(false);

        repository.save(entity);
    }

    private ChatMessage convertToDto(ChatMessageEntity entity) {
        ChatMessage dto = new ChatMessage();
        dto.setSender(entity.getSender());
        dto.setReceiver(entity.getReceiver());
        dto.setContent(entity.getContent());
        dto.setType(entity.getType());
        dto.setTime(entity.getTimestamp().toString());
        // ★★★ [即時已讀回執] 轉換已讀狀態 ★★★
        dto.setRead(entity.isRead());
        return dto;
    }
}