package com.taskmanager.account.adapter.in.rest;

import com.taskmanager.account.dto.AuthRequest;
import com.taskmanager.util.JwtUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil; // 新增 JwtUtil 的依賴

    public AuthController(AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public String login(@RequestBody AuthRequest authRequest, HttpSession session) {
        // 進行驗證
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getUsername(), authRequest.getPassword()));

        // 如果驗證成功，將 Authentication 放入安全上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 使用 JwtUtil 生成 Token
        String jwtToken = jwtUtil.generateToken(authRequest.getUsername());
        System.out.println("jwtToken : " + jwtToken);

        // 返回 Token
        return jwtToken;
    }
}
