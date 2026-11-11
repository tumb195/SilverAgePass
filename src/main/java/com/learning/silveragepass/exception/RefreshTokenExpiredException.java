package com.learning.silveragepass.exception;

/**
 * Refresh Token过期异常
 */
public class RefreshTokenExpiredException extends TokenException {

    public static final String ERROR_CODE = "REFRESH_TOKEN_EXPIRED";
    public static final int HTTP_STATUS = 401;

    public RefreshTokenExpiredException() {
        super("Refresh Token已过期", ERROR_CODE, HTTP_STATUS);
    }

    public RefreshTokenExpiredException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public RefreshTokenExpiredException(String message, Throwable cause) {
        super(message, ERROR_CODE, HTTP_STATUS, cause);
    }
}
