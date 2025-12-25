package com.taskmanager.account.adapter.in.rest;

import com.taskmanager.account.dto.AuthRequest;
import com.taskmanager.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Authentication API", description = "API for user authentication and JWT token generation")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates a user and returns a JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated, returns JWT token",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "401", description = "Invalid username or password",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    })
    public ResponseEntity<?> login(@RequestBody AuthRequest authRequest) {
        try {
            // 1. 嘗試驗證帳號密碼
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

            // 2. 驗證成功，設定 SecurityContext
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. 生成 Token
            String jwtToken = jwtUtil.generateToken(authRequest.getUsername());
            System.out.println("Login Success: " + authRequest.getUsername());

            // 4. 回傳 200 OK 與 Token
            return ResponseEntity.ok(jwtToken);

        } catch (BadCredentialsException e) {
            // ★★★ 關鍵修正：捕獲密碼錯誤，回傳 401 與 JSON 錯誤訊息 ★★★
            // 這樣前端 error.response.data.message 才能收到 "帳號或密碼錯誤"
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "帳號或密碼錯誤，請重新輸入"));

        } catch (Exception e) {
            // 捕獲其他預期外的錯誤 (例如資料庫連線失敗)
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "系統發生錯誤，請稍後再試"));
        }
    }
}