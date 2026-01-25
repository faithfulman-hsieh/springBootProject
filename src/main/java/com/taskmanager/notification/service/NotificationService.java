package com.taskmanager.notification.service;

import com.taskmanager.notification.dto.ChatMessage;
import com.taskmanager.notification.model.ChatMessageEntity;
import com.taskmanager.notification.repository.ChatMessageRepository;
import com.taskmanager.account.adapter.out.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
// 注意：這裡不需要 import Notification 了，因為我們改用 Data Message
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
    private final UserRepository userRepository;

    @Autowired
    public NotificationService(ChatMessageRepository repository,
                               SimpMessagingTemplate messagingTemplate,
                               UserRepository userRepository) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
        this.userRepository = userRepository;
    }

    public void sendGlobalMessage(ChatMessage message) {
        message.setTime(LocalDateTime.now().toString());
        saveMessage(message);
        messagingTemplate.convertAndSend("/topic/public-chat", message);
    }

    public void sendPrivateMessage(ChatMessage message) {
        message.setTime(LocalDateTime.now().toString());
        saveMessage(message);

        // 透過 WebSocket 發送給接收者
        messagingTemplate.convertAndSendToUser(message.getReceiver(), "/queue/messages", message);

        // 同步發送給寄件者自己 (Echo)
        messagingTemplate.convertAndSendToUser(message.getSender(), "/queue/messages", message);

        // ★★★ [Push] 觸發 FCM 推播 ★★★
        sendPushNotification(message);
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

    public long getUnreadCount(String sender, String receiver) {
        return repository.countBySenderAndReceiverAndIsReadFalse(sender, receiver);
    }

    public void markAsRead(String sender, String receiver) {
        repository.markMessagesAsRead(sender, receiver);
    }

    // ★★★ [Push] 實作推播發送邏輯 (改為 Data Message) ★★★
    private void sendPushNotification(ChatMessage message) {
        try {
            // 1. 查詢接收者的 FCM Token
            userRepository.findByUsername(message.getReceiver()).ifPresent(user -> {
                String token = user.getFcmToken();

                if (token != null && !token.isEmpty()) {
                    System.out.println("準備發送推播給: " + message.getReceiver());

                    // 2. 建構 Firebase 訊息 (使用 Data Message)
                    // 關鍵修改：不使用 .setNotification()，而是將標題與內容放入 putData
                    // 這樣前端 Service Worker 才能完全控制震動與通知行為
                    Message fcmMessage = Message.builder()
                            .setToken(token)
                            .putData("title", "新訊息來自 " + message.getSender())
                            .putData("body", message.getContent())
                            .putData("sender", message.getSender())
                            .putData("type", message.getType())
                            .build();

                    // 3. 發送
                    try {
                        String response = FirebaseMessaging.getInstance().send(fcmMessage);
                        System.out.println("推播發送成功, ID: " + response);
                    } catch (Exception e) {
                        System.err.println("推播發送失敗: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("使用者 " + message.getReceiver() + " 沒有 FCM Token，跳過推播");
                }
            });
        } catch (Exception e) {
            System.err.println("推播流程發生錯誤: " + e.getMessage());
        }
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
        entity.setTimestamp(LocalDateTime.now());
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
        dto.setRead(entity.isRead());
        return dto;
    }
}