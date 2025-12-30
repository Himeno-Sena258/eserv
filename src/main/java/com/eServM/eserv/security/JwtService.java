package com.eServM.eserv.security;

import com.eServM.eserv.repository.AdminApiKeyRepository;
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
    private final Key signingKey;
    private final Duration expiry;

    public JwtService(AdminApiKeyRepository adminApiKeyRepository,
                      @Value("${jwt.secret:}") String secret,
                      @Value("${jwt.exp.minutes:120}") long expMinutes) {
        this.adminApiKeyRepository = adminApiKeyRepository;
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("jwt.secret 未配置");
        }
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiry = Duration.ofMinutes(expMinutes);
    }

    public String createTokenForAdminKey(String keyValue) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(keyValue)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plus(expiry)))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        Claims claims = parseClaims(token);
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            return false;
        }
        return adminApiKeyRepository.findByKeyValueAndActiveTrue(subject.trim()).isPresent();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
