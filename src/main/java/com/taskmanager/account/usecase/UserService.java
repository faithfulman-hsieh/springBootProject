package com.taskmanager.account.usecase;

import com.taskmanager.account.adapter.out.repository.RoleRepository;
import com.taskmanager.account.adapter.out.repository.UserRepository;
import com.taskmanager.account.model.Role;
import com.taskmanager.account.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService implements UserUseCase {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public UserService(UserRepository userRepository, RoleRepository roleRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createUser(User user) {
        // 密碼加密邏輯
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);

        // 驗證並分配角色
        Set<Role> validatedRoles = validateAndAssignRoles(user.getRoles());
        user.setRoles(validatedRoles);

        // 儲存用戶
        return userRepository.save(user);
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    private Set<Role> validateAndAssignRoles(Set<Role> roles) {
        Set<Role> validatedRoles = new HashSet<>();
        for (Role role : roles) {
            Role foundRole = roleRepository.findById(role.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + role.getId()));
            validatedRoles.add(foundRole);
        }
        return validatedRoles;
    }

    @Override
    public User updateUser(Long userId, User user) {
        user.setId(userId);
        user.setPassword(encodePassword(user.getPassword())); // 確保更新時密碼也加密
        return userRepository.save(user);
    }

    @Override
    public User getUserById(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    @Override
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }

    @Override
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Set<Role> rolesToAssign = new HashSet<>();
        for (Long roleId : roleIds) {
            Role role = roleRepository.findById(roleId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + roleId));
            rolesToAssign.add(role);
        }
        user.setRoles(rolesToAssign);
        userRepository.save(user);
    }

    private String encodePassword(String rawPassword) {
        return new BCryptPasswordEncoder().encode(rawPassword); // 加密密碼
    }
}