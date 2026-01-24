package com.taskmanager.account.adapter.in.rest;

import com.taskmanager.account.model.User;
import com.taskmanager.account.usecase.UserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
@Tag(name = "User API", description = "API for managing users")
public class UserController {

    @Autowired
    private UserUseCase userUseCase;

    @GetMapping("/users")
    @Operation(summary = "Get all users", description = "Retrieves a list of all users")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved user list",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public List<User> getUsers(HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        System.out.println("Accessed by IP: " + clientIp);
        return userUseCase.getAllUsers();
    }

    @PostMapping("/addUser")
    @Operation(summary = "Add a new user", description = "Creates a new user with the provided details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User added successfully",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Invalid user data", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content)
    })
    public ResponseEntity<String> addUser(@RequestBody User newUser) {
        userUseCase.createUser(newUser);
        return ResponseEntity.ok("User added successfully!");
    }

    // ★★★ [FCM Token Storage] 修正邏輯：先查 ID 再更新，保持 Service 層一致性 ★★★
    @PostMapping("/fcm-token")
    public ResponseEntity<?> updateFcmToken(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        if (token == null || token.isEmpty()) {
            return ResponseEntity.badRequest().body("Token is required");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();

        // 先透過 username 找到 user ID
        Optional<User> userOpt = userUseCase.findByUsername(currentUsername);

        if (userOpt.isPresent()) {
            userUseCase.updateFcmToken(userOpt.get().getId(), token);
            return ResponseEntity.ok().body("FCM Token updated successfully");
        } else {
            return ResponseEntity.status(404).body("User not found");
        }
    }

}