package com.fivefy.common.lock.exception;

import lombok.Getter;

@Getter
public class ServiceException extends RuntimeException {

    private final ErrorEnum errorEnum;
    private final String customMessage;

    /**
     * [기본 생성자]
     * ErrorEnum에 정의된 기본 에러 메시지를 그대로 사용하는 경우
     *
     * 사용 예:
     * throw new ServiceException(ErrorEnum.INVALID_REQUEST);
     *
     * → ErrorEnum에 정의된 message 그대로 사용
     * @param errorEnum
     */
    public ServiceException(ErrorEnum errorEnum) {
        super(errorEnum.getErrorMessage());
        this.errorEnum = errorEnum;
        this.customMessage = errorEnum.getErrorMessage();
    }

    /**
     * ErrorEnum 메시지에 동적으로 값을 삽입해서 사용하는 경우
     *
     * 사용 예:
     * throw new ServiceException(ErrorEnum.NOT_FOUND_USER, userId);
     *
     * ErrorEnum 메시지:
     * "사용자를 찾을 수 없습니다. id=%s"
     *
     * → 결과 메시지:
     * "사용자를 찾을 수 없습니다. id=123"
     *
     * 내부적으로 String.format 사용
     * @param errorEnum
     * @param args
     */
    public ServiceException(ErrorEnum errorEnum, Object... args) {
        super(String.format(errorEnum.getErrorMessage(), args));
        this.errorEnum = errorEnum;
        this.customMessage = String.format(errorEnum.getErrorMessage(), args);
    }
}