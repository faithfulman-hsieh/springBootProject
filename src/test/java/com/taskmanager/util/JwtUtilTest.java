package com.taskmanager.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String token;
    private String username = "testuser";

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 生成測試用的 JWT
        token = jwtUtil.generateToken(username);
    }

    @Test
    void testGenerateToken() {
        assertNotNull(token);
        System.out.println("Generated Token: " + token);
    }

    @Test
    void testExtractUsername() {
        String extractedUsername = jwtUtil.extractUsername(token);
        assertEquals(username, extractedUsername);
    }

    @Test
    void testValidateToken() {
        boolean isValid = jwtUtil.validateToken(token, username);
        assertTrue(isValid);
    }

    @Test
    void testTokenExpiration() {
        boolean isExpired = jwtUtil.isTokenExpired(token);
        assertFalse(isExpired);
    }
}
