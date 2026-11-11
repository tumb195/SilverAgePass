package com.learning.silveragepass.service;

/**
 * 验证码服务接口
 */
public interface VerificationCodeService {
    
    /**
     * 保存图片验证码
     * @param sessionId 会话ID
     * @param code 验证码
     */
    void saveCaptchaCode(String sessionId, String code);
    
    /**
     * 获取图片验证码
     * @param sessionId 会话ID
     * @return 验证码
     */
    String getCaptchaCode(String sessionId);
    
    /**
     * 删除图片验证码
     * @param sessionId 会话ID
     */
    void removeCaptchaCode(String sessionId);
    
    /**
     * 生成短信验证码
     * @return 生成的验证码
     */
    String generateSmsCode();
    
    /**
     * 保存短信验证码
     * @param phone 手机号
     * @param code 验证码
     */
    void saveSmsCode(String phone, String code);
    
    /**
     * 获取短信验证码
     * @param phone 手机号
     * @return 验证码
     */
    String getSmsCode(String phone);
    
    /**
     * 删除短信验证码
     * @param phone 手机号
     */
    void removeSmsCode(String phone);
    
    /**
     * 验证图片验证码
     * @param sessionId 会话ID
     * @param inputCode 用户输入的验证码
     * @return 验证结果
     */
    boolean validateCaptchaCode(String sessionId, String inputCode);
    
    /**
     * 验证短信验证码
     * @param phone 手机号
     * @param inputCode 用户输入的验证码
     * @return 验证结果
     */
    boolean validateSmsCode(String phone, String inputCode);
    
    /**
     * 检查手机号是否可以发送验证码（是否超过发送间隔）
     * @param phone 手机号
     * @return 如果可以发送返回true，否则返回false
     */
    boolean canSendSmsCode(String phone);
    
    /**
     * 获取手机号距离下次可以发送验证码的剩余秒数
     * @param phone 手机号
     * @return 剩余秒数，如果可以发送则返回0
     */
    long getRemainingSendInterval(String phone);
    
    /**
     * 记录验证码发送时间
     * @param phone 手机号
     */
    void recordSmsSendTime(String phone);
} 