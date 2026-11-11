package com.learning.silveragepass.exception;

import lombok.Getter;

/**
 * Token异常基类
 */
@Getter
public class TokenException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public TokenException(String message, String errorCode, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public TokenException(String message, String errorCode, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
