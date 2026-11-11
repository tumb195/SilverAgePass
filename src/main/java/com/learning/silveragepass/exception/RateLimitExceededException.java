package com.learning.silveragepass.exception;

/**
 * 频率限制超限异常
 * 当用户请求频率超过限制时抛出
 * Requirements: 5.2 - 当用户每小时超过100次请求时，暂时阻止该用户的进一步请求
 */
public class RateLimitExceededException extends BusinessException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("KEY_002", "请求频率超限，请 " + retryAfterSeconds + " 秒后重试");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super("KEY_002", message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
