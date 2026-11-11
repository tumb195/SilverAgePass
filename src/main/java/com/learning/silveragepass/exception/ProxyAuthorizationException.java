package com.learning.silveragepass.exception;

/**
 * 代发授权相关异常类
 * 提供更具体的错误码和错误信息
 */
public class ProxyAuthorizationException extends BusinessException {

    /**
     * 代发授权相关错误码枚举
     */
    public enum ErrorCode {
        NO_RELATIONSHIP("PROXY_001", "您与该用户没有已接受的亲人关系"),
        DUPLICATE_REQUEST("PROXY_002", "您已有待处理或已批准的授权申请"),
        AUTHORIZATION_NOT_FOUND("PROXY_003", "授权申请不存在"),
        NOT_ELDER("PROXY_004", "只有老人本人才能处理授权申请"),
        ALREADY_PROCESSED("PROXY_005", "该授权申请已被处理"),
        NO_AUTHORIZATION("PROXY_006", "您没有代发需求的授权"),
        INSUFFICIENT_WARM_COIN("PROXY_007", "老人暖龄币余额不足");

        private final String code;
        private final String message;

        ErrorCode(String code, String message) {
            this.code = code;
            this.message = message;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }

    private final ErrorCode errorCode;

    public ProxyAuthorizationException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ProxyAuthorizationException(ErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
