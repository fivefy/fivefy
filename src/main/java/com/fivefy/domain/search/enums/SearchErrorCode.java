package com.fivefy.domain.search.enums;

import com.fivefy.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {
    ERR_SEARCH_KEYWORD_BLANK(HttpStatus.BAD_REQUEST, "검색어를 입력해주세요");

    private final HttpStatus httpStatus;
    private final String message;
}
