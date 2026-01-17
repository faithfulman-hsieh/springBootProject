package com.taskmanager.notification.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime timestamp;

    // 訊息類型 (CHAT, JOIN, LEAVE 等)
    private String type;

    // ★★★ [即時已讀回執] 新增資料庫欄位：是否已讀 ★★★
    @Column(name = "is_read")
    private boolean isRead = false;
}