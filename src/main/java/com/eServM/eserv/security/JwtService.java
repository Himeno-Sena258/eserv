package com.eServM.eserv.security;

import com.eServM.eserv.repository.AdminApiKeyRepository;
import com.eServM.eserv.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final AdminApiKeyRepository adminApiKeyRepository;
    private final UserRepository userRepository;
    private final Key signingKey;
    private final Duration expiry;

    public JwtService(AdminApiKeyRepository adminApiKeyRepository,
                      UserRepository userRepository,
                      @Value("${jwt.secret:}") String secret,
                      @Value("${jwt.exp.minutes:120}") long expMinutes) {
        this.adminApiKeyRepository = adminApiKeyRepository;
        this.userRepository = userRepository;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret 未配置");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiry = Duration.ofMinutes(expMinutes);
    }

    /** 签发管理员令牌 */
    public String createTokenForAdminKey(String keyValue) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(keyValue)
                .claim("role", "admin")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expiry)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 签发用户令牌 */
    public String createTokenForUser(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", "user")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expiry)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /** 校验令牌（支持 admin 与 user） */
    public boolean validateToken(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            return false;
        }
        String role = claims.get("role", String.class);
        if ("admin".equals(role)) {
            return adminApiKeyRepository.findByKeyValueAndActiveTrue(subject.trim()).isPresent();
        }
        if ("user".equals(role)) {
            return userRepository.findByUsernameAndActiveTrue(subject.trim()).isPresent();
        }
        return false;
    }

    /** 解析令牌 Claims */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
