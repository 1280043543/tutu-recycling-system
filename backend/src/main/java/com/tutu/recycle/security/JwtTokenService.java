package com.tutu.recycle.security;

import com.tutu.recycle.user.entity.SysUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtTokenService {
    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration-hours:12}")
    private long expirationHours;

    public String createToken(SysUser user) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationHours * 60L * 60L * 1000L);
        return Jwts.builder()
                .setSubject(String.valueOf(user.getId()))
                .claim("role", user.getRole())
                .setIssuedAt(now)
                .setExpiration(expiresAt)
                .signWith(SignatureAlgorithm.HS256, secret.getBytes(StandardCharsets.UTF_8))
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .setSigningKey(secret.getBytes(StandardCharsets.UTF_8))
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpirationSeconds() {
        return expirationHours * 60L * 60L;
    }
}
