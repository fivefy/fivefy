package com.fivefy.domain.notification.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {

    ERR_NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 알림입니다"),
    ERR_NOTIFICATION_UNAUTHORIZED(HttpStatus.FORBIDDEN, "알림에 접근할 권한이 없습니다");

    private final HttpStatus httpStatus;
    private final String message;
}
