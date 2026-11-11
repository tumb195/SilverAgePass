package com.learning.silveragepass.exception;

/**
 * 组织管理相关异常
 */
public class OrganizationException extends RuntimeException {

    private final String errorCode;

    public OrganizationException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // 预定义错误码
    public static final String ORG_NAME_EXISTS = "ORG_001";
    public static final String ORG_NOT_FOUND = "ORG_002";
    public static final String ORG_NAME_EMPTY = "ORG_003";
    public static final String ORG_NAME_TOO_LONG = "ORG_004";
    public static final String ORG_HAS_CERTIFICATIONS = "ORG_005";
    public static final String ORG_INACTIVE_OR_NOT_FOUND = "ORG_006";
}
