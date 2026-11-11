package com.learning.silveragepass.exception;

/**
 * Token格式错误异常
 */
public class MalformedTokenException extends TokenException {

    public static final String ERROR_CODE = "MALFORMED_TOKEN";
    public static final int HTTP_STATUS = 400;

    public MalformedTokenException() {
        super("Token格式错误", ERROR_CODE, HTTP_STATUS);
    }

    public MalformedTokenException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public MalformedTokenException(String message, Throwable cause) {
        super(message, ERROR_CODE, HTTP_STATUS, cause);
    }
}
