package com.taskmanager.notification.dto;

import lombok.Data;

@Data
public class ChatMessage {
    private String sender;
    private String receiver;
    private String content;
    private String type;
    private String time;
    private String data; // For WebRTC or other data

    // ★★★ [即時已讀回執] DTO 新增已讀欄位 ★★★
    private boolean read;
}