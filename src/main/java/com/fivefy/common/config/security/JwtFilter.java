package com.fivefy.common.config.security;

import com.fivefy.common.enums.AuthErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // Authorization 헤더에서 Bearer 토큰 추출
            String token = resolveToken(request);

            if (token == null) {
                // 토큰 없으면 그냥 통과 (Security가 처리)
                filterChain.doFilter(request, response);
                return;
            }

            // 검증된 토큰에서 사용자 정보 추출 후 SecurityContext에 인증 정보 저장
            Claims claims = jwtUtil.validateToken(token);
            UsernamePasswordAuthenticationToken authentication = jwtUtil.getAuthentication(claims);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (ExpiredJwtException e) {
            log.info("만료된 JWT 토큰입니다 : {}", e.getMessage());
            request.setAttribute("exception", AuthErrorCode.ERR_AUTH_EXPIRED_TOKEN);
        } catch (UnsupportedJwtException e) {
            log.info("지원하지 않는 JWT 토큰입니다 : {}", e.getMessage());
            request.setAttribute("exception", AuthErrorCode.ERR_AUTH_INVALID_TOKEN);
        } catch (MalformedJwtException e) {
            log.info("잘못된 JWT 토큰입니다 : {}", e.getMessage());
            request.setAttribute("exception", AuthErrorCode.ERR_AUTH_INVALID_TOKEN);
        } catch (IllegalArgumentException e) {
            log.info("JWT 토큰이 비어있습니다 : {}", e.getMessage());
            request.setAttribute("exception", AuthErrorCode.ERR_AUTH_EMPTY_TOKEN);
        } catch (Exception e) {
            log.error("인증 처리 중 오류 발생 : {}", e.getMessage());
            request.setAttribute("exception", AuthErrorCode.ERR_AUTH_UNAUTHORIZED);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
