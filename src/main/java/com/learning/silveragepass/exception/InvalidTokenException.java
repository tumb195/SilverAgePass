package com.learning.silveragepass.exception;

/**
 * 无效Token异常（签名验证失败）
 */
public class InvalidTokenException extends TokenException {

    public static final String ERROR_CODE = "INVALID_TOKEN";
    public static final int HTTP_STATUS = 401;

    public InvalidTokenException() {
        super("Token签名无效", ERROR_CODE, HTTP_STATUS);
    }

    public InvalidTokenException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, ERROR_CODE, HTTP_STATUS, cause);
    }
}
