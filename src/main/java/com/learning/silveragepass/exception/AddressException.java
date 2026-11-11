package com.learning.silveragepass.exception;

/**
 * 地址管理相关异常类
 * 提供更具体的错误码和错误信息
 */
public class AddressException extends BusinessException {

    /**
     * 地址相关错误码枚举
     */
    public enum ErrorCode {
        ADDRESS_NOT_FOUND("ADDRESS_NOT_FOUND", "地址不存在或已被删除"),
        ADDRESS_LIMIT_EXCEEDED("ADDRESS_LIMIT_EXCEEDED", "地址数量已达上限，最多可保存20个地址"),
        ADDRESS_DUPLICATE("ADDRESS_DUPLICATE", "该地址已存在，请勿重复添加"),
        ADDRESS_ACCESS_DENIED("ADDRESS_ACCESS_DENIED", "无权操作此地址"),
        ADDRESS_INVALID_FORMAT("ADDRESS_INVALID_FORMAT", "地址格式不正确"),
        ADDRESS_LABEL_TOO_LONG("ADDRESS_LABEL_TOO_LONG", "地址标签长度不能超过20个字符"),
        ADDRESS_TOO_LONG("ADDRESS_TOO_LONG", "地址长度不能超过255个字符"),
        ADDRESS_EMPTY("ADDRESS_EMPTY", "地址不能为空"),
        INVALID_COORDINATES("INVALID_COORDINATES", "经纬度坐标无效"),
        DEFAULT_ADDRESS_NOT_SET("DEFAULT_ADDRESS_NOT_SET", "尚未设置默认地址");

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

    public AddressException(ErrorCode errorCode) {
        super(errorCode.getCode(), errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AddressException(ErrorCode errorCode, String customMessage) {
        super(errorCode.getCode(), customMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
