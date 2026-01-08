package com.taskmanager.notification.repository;

import com.taskmanager.notification.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    // 1. 查詢兩人之間的對話紀錄 (A->B 或 B->A)
    @Query("SELECT m FROM ChatMessageEntity m WHERE " +
            "(m.sender = :user1 AND m.receiver = :user2) OR " +
            "(m.sender = :user2 AND m.receiver = :user1) " +
            "ORDER BY m.sendTime ASC")
    List<ChatMessageEntity> findChatHistory(String user1, String user2);

    // 2. 統計某人(sender) 發給 我(receiver) 的未讀數量
    long countBySenderAndReceiverAndIsReadFalse(String sender, String receiver);

    // 3. 將某人發給我的訊息全部標為已讀
    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true WHERE m.sender = :sender AND m.receiver = :receiver")
    void markMessagesAsRead(String sender, String receiver);
}