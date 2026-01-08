package com.taskmanager.notification.dto;

public class ChatMessage {
    private String sender;
    private String receiver; // 指定接收者 (語音通常是 1對1)
    private String content;  // 聊天內容
    private String type;     // CHAT, JOIN, LEAVE, NOTIFICATION, OFFER, ANSWER, CANDIDATE, HANGUP
    private String data;     // WebRTC 信令數據 (SDP/ICE string)
    private String time;

    public ChatMessage() {}

    public ChatMessage(String sender, String content, String type, String time) {
        this.sender = sender;
        this.content = content;
        this.type = type;
        this.time = time;
    }

    // Getters and Setters
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReceiver() { return receiver; }
    public void setReceiver(String receiver) { this.receiver = receiver; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getData() { return data; }
    public void setData(String data) { this.data = data; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
}