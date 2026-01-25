package com.taskmanager.notification.service;

import com.taskmanager.notification.dto.ChatMessage;
import com.taskmanager.notification.model.ChatMessageEntity;
import com.taskmanager.notification.repository.ChatMessageRepository;
import com.taskmanager.account.adapter.out.repository.UserRepository;
import com.google.firebase.messaging.AndroidConfig; // ★★★ 新增 ★★★
import com.google.firebase.messaging.AndroidNotification; // ★★★ 新增 ★★★
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
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

        messagingTemplate.convertAndSendToUser(message.getReceiver(), "/queue/messages", message);
        messagingTemplate.convertAndSendToUser(message.getSender(), "/queue/messages", message);

        // 發送高優先級推播
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

    // ★★★ [Push] 改良版：加入 Android High Priority 設定 ★★★
    public void sendPushNotification(ChatMessage message) {
        try {
            userRepository.findByUsername(message.getReceiver()).ifPresent(user -> {
                String token = user.getFcmToken();

                if (token != null && !token.isEmpty()) {
                    System.out.println("準備發送推播給: " + message.getReceiver() + " (Type: " + message.getType() + ")");

                    String title = "新訊息來自 " + message.getSender();
                    String body = message.getContent();

                    // 針對通話信令的特殊文字處理
                    if ("OFFER".equals(message.getType())) {
                        title = "來電通知";
                        body = message.getSender() + " 邀請您進行視訊通話...";
                    } else if ("HANGUP".equals(message.getType())) {
                        title = "通話結束";
                        body = "未接來電";
                    }

                    Message fcmMessage = Message.builder()
                            .setToken(token)
                            // 1. Data 區塊 (前端 SW 讀取用)
                            .putData("title", title)
                            .putData("body", body)
                            .putData("sender", message.getSender())
                            .putData("type", message.getType())

                            // 2. ★★★ Android 專用設定 (關鍵) ★★★
                            // 設定 priority 為 HIGH，確保手機在休眠(Doze)模式下也能立刻收到並執行 Service Worker
                            .setAndroidConfig(AndroidConfig.builder()
                                    .setPriority(AndroidConfig.Priority.HIGH)
                                    .setTtl(0) // 0 代表即時傳送，過期不候 (適合通話)
                                    .build())
                            .build();

                    try {
                        String response = FirebaseMessaging.getInstance().send(fcmMessage);
                        System.out.println("推播發送成功, ID: " + response);
                    } catch (Exception e) {
                        System.err.println("推播發送失敗: " + e.getMessage());
                        e.printStackTrace();
                    }
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