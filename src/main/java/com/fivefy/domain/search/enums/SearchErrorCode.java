package com.fivefy.domain.search.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {
    ERR_SEARCH_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 검색기록 입니다"),
    ERR_SEARCH_KEYWORD_BLANK(HttpStatus.BAD_REQUEST, "검색어를 입력해주세요"),
    ERR_SEARCH_KEYWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "검색어는 2글자 이상 입력해주세요");

    private final HttpStatus httpStatus;
    private final String message;
}
