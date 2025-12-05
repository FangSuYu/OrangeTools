package cn.orangetools.common.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

/**
 * @author YuHeng
 * @project backend
 * @file JwtUtils
 * @date 2025/12/5 20:24
 * @github https://github.com/FangSuYu/OrangeTools.git
 * @license GPL-3.0 License
 */
@Component // 注册为 Bean，方便在其他地方注入
public class JwtUtils {

    // 1. 密钥：这就像印钞厂的母版，绝对不能泄露！
    // 注意：jjwt 0.12.x 要求密钥长度必须足够长（至少32个字符），否则会报错。
    @Value("${jwt.secret}")
    private static String SECRET_KEY;

    // 2. Token 有效期：这里设为 24 小时 (毫秒)
    @Value("${jwt.expiration}")
    private static long EXPIRATION_TIME;

    // 生成安全密钥对象
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    // ================== 核心功能 1：生成 Token ==================
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username) // 把用户名存进 Token
                .issuedAt(new Date(System.currentTimeMillis())) // 签发时间
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 过期时间
                .signWith(getSigningKey()) // 盖章（签名）
                .compact(); // 压缩成字符串
    }

    // ================== 核心功能 2：解析 Token ==================
    // 从 Token 中提取用户名
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    // 验证 Token 是否有效
    public boolean isTokenValid(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }

    // --- 内部辅助方法 ---

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 使用公钥验证签名
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
