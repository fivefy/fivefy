package com.fivefy.common.config.security;

import com.fivefy.domain.user.enums.UserRole;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

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

    // token 검증
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다 : {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.info("지원하지 않는 JWT 토큰입니다 : {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.info("잘못된 JWT 토큰입니다 : {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 비어있습니다 : {}", e.getMessage());
        }
        return false;
    }

    // claims 추출
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // userId 추출
    public Long getUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    // role 추출
    public UserRole getRole(String token) {
        return UserRole.valueOf(getClaims(token).get("role", String.class));
    }
}
