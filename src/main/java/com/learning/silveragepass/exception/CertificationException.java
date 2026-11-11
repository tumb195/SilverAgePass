package com.learning.silveragepass.exception;

/**
 * 组织认证相关异常类
 * 提供更具体的错误码和错误信息
 * 
 * Validates: Requirements 1.2, 1.3, 5.2
 */
public class CertificationException extends BusinessException {

    /**
     * 组织认证相关错误码枚举
     */
    public enum ErrorCode {
        PENDING_EXISTS("CERT_001", "已有待审核的认证申请"),
        ALREADY_APPROVED("CERT_002", "已通过认证，无需重复申请"),
        NOT_FOUND("CERT_003", "认证申请不存在"),
        INVALID_STATUS("CERT_004", "只能审核待审核状态的申请"),
        NOT_CERTIFIED("CERT_005", "用户未通过组织认证，无法接单"),
        INVALID_INPUT("CERT_006", "输入参数无效");

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

    public CertificationException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CertificationException(ErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
