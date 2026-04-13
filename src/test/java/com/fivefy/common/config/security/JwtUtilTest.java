package com.fivefy.common.config.security;

import com.fivefy.domain.user.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(
                "test-secret-key-must-be-at-least-32-bytes!!",
                1800000L,  // 30분
                604800000L // 7일
        );
        jwtUtil = new JwtUtil(properties);
    }

    @Test
    @DisplayName("다른 서명키로 만든 토큰은 SignatureException 발생")
    void anotherSigningKeyThrowSignatureException() {
        JwtProperties otherProperties = new JwtProperties(
                "other-secret-key-must-be-at-least-32-bytes!!",
                1800000L,
                604800000L
        );
        JwtUtil otherJwtUtil = new JwtUtil(otherProperties);
        String token = otherJwtUtil.createAccessToken(1L, UserRole.USER);

        assertThatThrownBy(() -> jwtUtil.validateToken(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("AccessToken을 생성하고 검증")
    void createAndValidateAT() {
        String token = jwtUtil.createAccessToken(1L, UserRole.USER);
        Claims claims = jwtUtil.validateToken(token);

        assertThat(Long.parseLong(claims.getSubject())).isEqualTo(1L);
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    @DisplayName("RefreshToken은 subject와 role 클레임 없이 생성")
    void createRefreshTokenWithoutSubjectAndRole() {
        String token = jwtUtil.createRefreshToken();
        Claims claims = jwtUtil.validateToken(token);

        assertThat(claims.getSubject()).isNull();
        assertThat(claims.get("role")).isNull();
    }

    @Test
    @DisplayName("만료된 토큰은 ExpiredJwtException 발생")
    void expiredTokenThrowExpiredJwtException() {
        JwtProperties expiredProperties = new JwtProperties(
                "test-secret-key-must-be-at-least-32-bytes!!",
                -1L, // 이미 만료
                604800000L
        );
        JwtUtil expiredJwtUtil = new JwtUtil(expiredProperties);
        String token = expiredJwtUtil.createAccessToken(1L, UserRole.USER);

        assertThatThrownBy(() -> jwtUtil.validateToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("claims로 authentication 객체 생성")
    void createAuthenticationWithClaims() {
        String token = jwtUtil.createAccessToken(1L, UserRole.USER);
        Claims claims = jwtUtil.validateToken(token);

        UsernamePasswordAuthenticationToken authentication =
                jwtUtil.getAuthentication(claims);

        assertThat(authentication.getPrincipal()).isEqualTo(1L);
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }
}