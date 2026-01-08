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

    private String sender;    // 發送者 username
    private String receiver;  // 接收者 username (null 代表廣播)

    @Column(columnDefinition = "TEXT")
    private String content;

    private String type;      // CHAT, JOIN, LEAVE...

    private LocalDateTime sendTime;

    private boolean isRead = false; // 未讀狀態
}