package com.example.demo.controller;

import com.example.demo.model.Role;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.demo.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository; // 使用 H2 資料庫的 repository 來進行操作
    @Autowired
    private RoleRepository roleRepository; // 使用 H2 資料庫的 repository 來進行操作

    // BCryptPasswordEncoder 用於加密密碼
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    // 調整後的 getUsers 方法，從 H2 資料庫中獲取用戶資料
    @GetMapping("/users")
    public List<User> getUsers(HttpServletRequest request) {
        // 記錄訪問者的 IP 地址
        String clientIp = request.getRemoteAddr();
        System.out.println("Accessed by IP: " + clientIp);

        // 從 H2 資料庫中獲取用戶列表
        List<User> users = userRepository.findAll();

        // 如果 H2 資料庫中沒有用戶資料，就返回空列表
        if (users.isEmpty()) {
            users = new ArrayList<>();
            System.out.println("No users found in H2.");
        }

        // 記錄回傳的 JSON 格式
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String jsonOutput = objectMapper.writeValueAsString(users);
            System.out.println("Returned JSON: " + jsonOutput);
        } catch (Exception e) {
            System.err.println("Error converting users to JSON: " + e.getMessage());
        }

        return users; // 回傳用戶資料
    }

    @PostMapping("/addUser")
    public ResponseEntity<String> addUser(@RequestBody User newUser) {
        Set<Role> rolesToAssign = new HashSet<>();

        // 加密密碼
        String encodedPassword = passwordEncoder.encode(newUser.getPassword());
        newUser.setPassword(encodedPassword); // 設置加密後的密碼

        // 遍歷新用戶的角色，確保不會重複添加相同角色
        for (Role role : newUser.getRoles()) {
            Role foundRole = roleRepository.findById(role.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid role ID: " + role.getId()));
            rolesToAssign.add(foundRole);
        }

        // 設置用戶的角色
        newUser.setRoles(rolesToAssign);
        userRepository.save(newUser);
        return ResponseEntity.ok("User added successfully!");
    }
}
