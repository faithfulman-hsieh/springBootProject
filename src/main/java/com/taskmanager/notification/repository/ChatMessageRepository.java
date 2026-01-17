package com.taskmanager.notification.repository;

import com.taskmanager.notification.model.ChatMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    // 查詢公開歷史紀錄
    List<ChatMessageEntity> findByReceiverIsNullOrderByTimestampAsc();

    // 查詢私訊歷史紀錄
    @Query("SELECT m FROM ChatMessageEntity m WHERE (m.sender = :user1 AND m.receiver = :user2) OR (m.sender = :user2 AND m.receiver = :user1) ORDER BY m.timestamp ASC")
    List<ChatMessageEntity> findPrivateMessages(String user1, String user2);

    // ★★★ [即時已讀回執] 查詢未讀數量 ★★★
    long countBySenderAndReceiverAndIsReadFalse(String sender, String receiver);

    // ★★★ [即時已讀回執] 更新已讀狀態 ★★★
    @Modifying
    @Transactional
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true WHERE m.sender = :sender AND m.receiver = :receiver AND m.isRead = false")
    void markMessagesAsRead(String sender, String receiver);
}