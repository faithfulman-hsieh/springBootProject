package com.taskmanager.account.adapter.out.repository;

import com.taskmanager.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // ★★★ [Fix] 修正方法名稱以對應 User.username 欄位 ★★★
    // 舊的 findByName 會導致 "No property name found" 錯誤，必須移除或更名
    Optional<User> findByUsername(String username);
}