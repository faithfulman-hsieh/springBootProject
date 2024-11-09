package com.taskmanager.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtUtil {

    // 生成 256 位元的安全密鑰
    private SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 生成 JWT Token
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    // 創建 Token
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 設置 10 小時有效期
                .signWith(SECRET_KEY)  // 使用新的安全密鑰
                .compact();
    }

    // 提取 Token 中的用戶名
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 提取 Token 中的任意聲明（Claim）
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // 解析所有的 Claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
    }

    // 驗證 Token 是否過期
    public Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // 提取 Token 的過期時間
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // 驗證 Token
    public Boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
