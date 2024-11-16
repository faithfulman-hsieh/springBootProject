package com.taskmanager.account.adapter.in.rest;

import com.taskmanager.account.model.User;
import com.taskmanager.account.usecase.UserUseCase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserUseCase userUseCase; // 使用接口以利測試和解耦

//    // Constructor Injection
//    public UserController(UserUseCase userUseCase) {
//        this.userUseCase = userUseCase;
//    }

    @GetMapping("/users")
    public List<User> getUsers(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        System.out.println("Accessed by IP: " + clientIp);
        return userUseCase.getAllUsers();
    }

    @PostMapping("/addUser")
    public ResponseEntity<String> addUser(@RequestBody User newUser) {
        userUseCase.createUser(newUser);
        return ResponseEntity.ok("User added successfully!");
    }
}