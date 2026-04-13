package com.fivefy.common.config.security;

import com.fivefy.domain.user.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    // 평문 secret key로 서명 키 생성
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    // access token 생성
    public String createAccessToken(Long userId, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.accessExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    // refresh token 생성
    public String createRefreshToken() {
        Date now = new Date();
        return Jwts.builder()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + jwtProperties.refreshExpiration()))
                .signWith(getSigningKey())
                .compact();
    }

    // token 검증 성공 시 claims 반환, 실패 시 필터에서 예외 저장
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // claims에서 authentication 객체 생성
    public UsernamePasswordAuthenticationToken getAuthentication(Claims claims) {
        Long userId = Long.parseLong(claims.getSubject());
        UserRole role = UserRole.valueOf(claims.get("role", String.class));

        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));

        return new UsernamePasswordAuthenticationToken(userId, null, authorities);
    }
}
