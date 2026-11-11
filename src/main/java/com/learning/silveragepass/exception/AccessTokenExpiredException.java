package com.learning.silveragepass.exception;

/**
 * Access Token过期异常
 */
public class AccessTokenExpiredException extends TokenException {

    public static final String ERROR_CODE = "ACCESS_TOKEN_EXPIRED";
    public static final int HTTP_STATUS = 401;

    public AccessTokenExpiredException() {
        super("Access Token已过期", ERROR_CODE, HTTP_STATUS);
    }

    public AccessTokenExpiredException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public AccessTokenExpiredException(String message, Throwable cause) {
        super(message, ERROR_CODE, HTTP_STATUS, cause);
    }
}
