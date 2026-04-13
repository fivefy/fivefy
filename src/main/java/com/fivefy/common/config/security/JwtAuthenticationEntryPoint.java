package com.fivefy.common.config.security;

import com.fivefy.common.dto.response.BaseResponse;
import com.fivefy.common.enums.AuthErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            @NonNull AuthenticationException authException
    ) throws IOException, ServletException {
        // 필터에서 저장한 예외 정보로 에러 코드 결정
        Object exception = request.getAttribute("exception");
        AuthErrorCode errorCode = (exception instanceof AuthErrorCode authErrorCode)
                ? authErrorCode
                : AuthErrorCode.ERR_AUTH_UNAUTHORIZED;

        log.info("JwtAuthenticationEntryPoint 호출 : {}", errorCode.getMessage());

        // 공통 에러 응답 반환
        BaseResponse<Void> errorResponse = BaseResponse.fail(errorCode.getHttpStatus(), errorCode.getMessage());
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
