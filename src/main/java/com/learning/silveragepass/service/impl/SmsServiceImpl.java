package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.service.SmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SmsServiceImpl implements SmsService {
    
    private static final Logger logger = LoggerFactory.getLogger(SmsServiceImpl.class);
    
    @Override
    public boolean sendVerificationCode(String phone, String code) {
        // 这里应该调用实际的短信服务提供商API发送短信
        // 例如阿里云短信、腾讯云短信等
        
        // 目前仅打印日志，模拟发送短信
        logger.info("向手机号 {} 发送验证码: {}", phone, code);
        
        // 返回发送成功
        return true;
    }
} 