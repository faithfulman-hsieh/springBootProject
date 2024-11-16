package com.taskmanager.account.usecase;

import com.taskmanager.account.model.User;

import java.util.List;

public interface UserUseCase {
    User createUser(User user);
    User updateUser(Long userId, User user);
    User getUserById(Long userId);
    List<User> getAllUsers();
    void deleteUser(Long userId);

    // 新增角色相關方法
    void assignRolesToUser(Long userId, List<Long> roleIds);
}