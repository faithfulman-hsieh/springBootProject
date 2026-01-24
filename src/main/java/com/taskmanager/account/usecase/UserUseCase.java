package com.taskmanager.account.usecase;

import com.taskmanager.account.model.User;

import java.util.List;
import java.util.Optional;

public interface UserUseCase {
    User createUser(User user);
    User updateUser(Long userId, User user);
    User getUserById(Long userId);
    List<User> getAllUsers();
    void deleteUser(Long userId);

    // 新增角色相關方法
    void assignRolesToUser(Long userId, List<Long> roleIds);

    // 補上介面定義中可能缺漏的 findByUsername (Controller 會用到)
    Optional<User> findByUsername(String username);

    // ★★★ [FCM Token Storage] 改用 userId 以保持一致性 ★★★
    void updateFcmToken(Long userId, String token);
}