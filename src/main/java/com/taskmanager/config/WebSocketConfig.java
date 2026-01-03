package com.taskmanager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 設定 WebSocket 連線端點，允許跨域
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 啟用 SockJS 作為備援
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 設定訊息代理的前綴
        // /topic: 廣播訊息 (如：聊天室)
        // /queue: 點對點訊息 (如：個人通知、語音信令)
        registry.enableSimpleBroker("/topic", "/queue");

        // 設定客戶端發送訊息的前綴 (例如前端送給後端)
        registry.setApplicationDestinationPrefixes("/app");

        // 設定點對點推播的前綴 (給特定使用者發通知用)
        registry.setUserDestinationPrefix("/user");
    }
}