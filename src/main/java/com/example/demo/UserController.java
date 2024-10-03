package com.example.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api")
public class UserController {

    private final String[] names = {
            "John Doe", "Jane Smith", "Alice Johnson", "Bob Brown",
            "Charlie Davis", "David Wilson", "Eva Adams", "Fiona Green",
            "George Black", "Hannah White"
    };

    @GetMapping("/users")
    public List<User> getUsers(HttpServletRequest request) {
        // 記錄訪問者的 IP 地址
        String clientIp = request.getRemoteAddr();
        System.out.println("Accessed by IP: " + clientIp);

        // 隨機生成 10 筆用戶資料
        List<User> users = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 10; i++) {
            int id = i + 1;
            String name = names[random.nextInt(names.length)];
            String email = name.toLowerCase().replace(" ", ".") + "@example.com";
            users.add(new User(id, name, email));
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
}
