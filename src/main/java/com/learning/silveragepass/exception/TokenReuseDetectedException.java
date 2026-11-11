package com.learning.silveragepass.exception;

/**
 * Token重放攻击检测异常
 */
public class TokenReuseDetectedException extends TokenException {

    public static final String ERROR_CODE = "TOKEN_REUSE_DETECTED";
    public static final int HTTP_STATUS = 401;

    public TokenReuseDetectedException() {
        super("检测到Token重放攻击，所有会话已被终止", ERROR_CODE, HTTP_STATUS);
    }

    public TokenReuseDetectedException(String message) {
        super(message, ERROR_CODE, HTTP_STATUS);
    }

    public TokenReuseDetectedException(String message, Throwable cause) {
        super(message, ERROR_CODE, HTTP_STATUS, cause);
    }
}
