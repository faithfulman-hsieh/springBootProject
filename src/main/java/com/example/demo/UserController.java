package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @GetMapping("/users")
    public List<User> getUsers() {
        // 這裡應該從服務層或數據源獲取用戶資料
        System.out.println("This is an getUsers message.");
        return Arrays.asList(new User(1, "John Doe", "john.doe@example.com"));
    }
}
