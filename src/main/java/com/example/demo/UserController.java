package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository; // 使用 H2 資料庫的 repository 來進行操作

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

    // 調整後的 addUser 方法，將用戶資料新增到 H2 資料庫
    @PostMapping("/addUser")
    public String addUser(@RequestBody User newUser) {
        // 將新用戶存入 H2 資料庫
        userRepository.save(newUser);

        System.out.println("Added new user: " + newUser.getName() + ", Email: " + newUser.getEmail());
        return "User added successfully!";
    }
}
