package com.taskmanager.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Request object for user authentication")
public class AuthRequest {

    @Schema(description = "Username of the user", example = "john_doe", required = true)
    private String username;

    @Schema(description = "Password of the user", example = "password123", required = true)
    private String password;

    // 無參構造函數（必要，Spring 需要）
    public AuthRequest() {
    }

    // 有參構造函數（如果需要初始化時用）
    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getter 和 Setter
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}