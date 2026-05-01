package com.lumina.util;

import com.lumina.config.LuminaProperties;
import com.lumina.dto.LoginResponse;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JwtUtil {

    @Autowired
    private LuminaProperties luminaProperties;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private volatile SecretKey signingKey;

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            synchronized (this) {
                if (signingKey == null) {
                    // 使用配置的密钥字符串生成一个符合HS512要求的安全密钥
                    String secret = luminaProperties.getAuth().getJwt().getSecret();
                    
                    // 使用SHA-256哈希处理配置的密钥，然后扩展到512位(64字节)以满足HS512要求
                    try {
                        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                        byte[] keyBytes = sha256.digest(secret.getBytes());
                        
                        // 创建一个64字节的数组来满足HS512的要求
                        byte[] expandedKey = new byte[64];
                        System.arraycopy(keyBytes, 0, expandedKey, 0, Math.min(keyBytes.length, 64));
                        
                        signingKey = Keys.hmacShaKeyFor(expandedKey);
                    } catch (NoSuchAlgorithmException e) {
                        throw new RuntimeException("Failed to generate signing key", e);
                    }
                }
            }
        }
        return signingKey;
    }

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    /**
     * 生成 Access Token
     */
    public String generateToken(String username) {
        return generateToken(username, TYPE_ACCESS, luminaProperties.getAuth().getJwt().getExpiration());
    }

    /**
     * 生成 Refresh Token
     */
    public String generateRefreshToken(String username) {
        return generateToken(username, TYPE_REFRESH, luminaProperties.getAuth().getJwt().getRefreshExpiration());
    }

    private String generateToken(String username, String type, long expirationMs) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expirationMs);

        String token = Jwts.builder()
                .setSubject(username)
                .claim(CLAIM_TYPE, type)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();

        // 将 token 存储到 Redis 中，用于后续的注销功能
        redisTemplate.opsForValue().set(
                "jwt:token:" + username + ":" + token,
                type,
                expirationMs,
                TimeUnit.MILLISECONDS
        );

        return token;
    }

    /**
     * 从 token 中获取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * 验证 Access Token 是否有效（用于 API 请求鉴权）
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, TYPE_ACCESS);
    }

    /**
     * 验证 Refresh Token 是否有效（用于刷新令牌）
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, TYPE_REFRESH);
    }

    private boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            String tokenType = claims.get(CLAIM_TYPE, String.class);

            // 校验 token 类型
            if (!expectedType.equals(tokenType)) {
                log.warn("Token type mismatch: expected={}, actual={}", expectedType, tokenType);
                return false;
            }

            // 检查 token 是否存在于 Redis 中（用于注销验证）
            String redisTokenType = redisTemplate.opsForValue().get("jwt:token:" + username + ":" + token);
            if (redisTokenType == null || !redisTokenType.equals(expectedType)) {
                return false;
            }

            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 检查 token 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    /**
     * 注销 token
     */
    public void revokeToken(String token) {
        try {
            String username = getUsernameFromToken(token);
            redisTemplate.delete("jwt:token:" + username + ":" + token);
            log.info("Token revoked for user: {}", username);
        } catch (Exception e) {
            log.error("Error revoking token: {}", e.getMessage());
        }
    }

    /**
     * 刷新 token
     * 先注销旧 refresh token，再签发新 token 对，防止旧 token 被重复使用
     */
    public LoginResponse refreshToken(String refreshToken) {
        try {
            if (!validateRefreshToken(refreshToken)) {
                return null;
            }

            String username = getUsernameFromToken(refreshToken);

            // 先注销旧的 refresh token，关闭竞态窗口
            revokeToken(refreshToken);

            // 签发新的 token 对
            String newToken = generateToken(username);
            String newRefreshToken = generateRefreshToken(username);

            return LoginResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .expiresIn(luminaProperties.getAuth().getJwt().getExpiration())
                .username(username)
                .build();
        } catch (Exception e) {
            log.error("Error refreshing token: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 从请求头中获取 token
     */
    public String getTokenFromHeader(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}