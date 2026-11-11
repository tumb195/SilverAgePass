package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.service.VerificationCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 验证码服务实现类
 */
@Service
public class VerificationCodeServiceImpl implements VerificationCodeService {

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;
    
    // 内存缓存，用于测试环境（无Redis时）
    private final ConcurrentHashMap<String, String> memoryCache = new ConcurrentHashMap<>();
    
    // Redis中验证码的前缀
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String SMS_CODE_PREFIX = "sms:";
    private static final String SMS_SEND_TIME_PREFIX = "sms:send_time:";
    
    // 验证码过期时间（分钟）
    private static final long CAPTCHA_EXPIRATION = 5;
    private static final long SMS_CODE_EXPIRATION = 5;
    
    // 短信发送间隔时间（秒）
    private static final long SMS_SEND_INTERVAL = 60;

    @Override
    public void saveCaptchaCode(String sessionId, String code) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(CAPTCHA_PREFIX + sessionId, code, CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        } else {
            memoryCache.put(CAPTCHA_PREFIX + sessionId, code);
        }
    }

    @Override
    public String getCaptchaCode(String sessionId) {
        if (redisTemplate != null) {
            Object code = redisTemplate.opsForValue().get(CAPTCHA_PREFIX + sessionId);
            return code != null ? code.toString() : null;
        } else {
            return memoryCache.get(CAPTCHA_PREFIX + sessionId);
        }
    }

    @Override
    public void removeCaptchaCode(String sessionId) {
        if (redisTemplate != null) {
            redisTemplate.delete(CAPTCHA_PREFIX + sessionId);
        } else {
            memoryCache.remove(CAPTCHA_PREFIX + sessionId);
        }
    }

    @Override
    public String generateSmsCode() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    @Override
    public void saveSmsCode(String phone, String code) {
        if (redisTemplate != null) {
            redisTemplate.opsForValue().set(SMS_CODE_PREFIX + phone, code, SMS_CODE_EXPIRATION, TimeUnit.MINUTES);
        } else {
            memoryCache.put(SMS_CODE_PREFIX + phone, code);
        }
    }

    @Override
    public String getSmsCode(String phone) {
        if (redisTemplate != null) {
            Object code = redisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
            return code != null ? code.toString() : null;
        } else {
            return memoryCache.get(SMS_CODE_PREFIX + phone);
        }
    }

    @Override
    public void removeSmsCode(String phone) {
        if (redisTemplate != null) {
            redisTemplate.delete(SMS_CODE_PREFIX + phone);
        } else {
            memoryCache.remove(SMS_CODE_PREFIX + phone);
        }
    }

    @Override
    public boolean validateCaptchaCode(String sessionId, String inputCode) {
        String storedCode = getCaptchaCode(sessionId);
        if (storedCode == null) {
            return false;
        }
        
        boolean isValid = storedCode.equalsIgnoreCase(inputCode);
        if (isValid) {
            removeCaptchaCode(sessionId);
        }
        return isValid;
    }

    @Override
    public boolean validateSmsCode(String phone, String inputCode) {
        String storedCode = getSmsCode(phone);
        if (storedCode == null) {
            return false;
        }
        
        boolean isValid = storedCode.equals(inputCode);
        if (isValid) {
            removeSmsCode(phone);
        }
        return isValid;
    }

    @Override
    public boolean canSendSmsCode(String phone) {
        String key = SMS_SEND_TIME_PREFIX + phone;
        Object lastSendTime;
        
        if (redisTemplate != null) {
            lastSendTime = redisTemplate.opsForValue().get(key);
        } else {
            lastSendTime = memoryCache.get(key);
        }
        
        if (lastSendTime == null) {
            // 没有发送记录，可以发送
            return true;
        }
        
        try {
            long lastTime = Long.parseLong(lastSendTime.toString());
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - lastTime) / 1000;
            
            // 判断是否超过发送间隔
            return elapsedSeconds >= SMS_SEND_INTERVAL;
        } catch (NumberFormatException e) {
            // 数据格式错误，允许发送
            return true;
        }
    }

    @Override
    public long getRemainingSendInterval(String phone) {
        String key = SMS_SEND_TIME_PREFIX + phone;
        Object lastSendTime;
        
        if (redisTemplate != null) {
            lastSendTime = redisTemplate.opsForValue().get(key);
        } else {
            lastSendTime = memoryCache.get(key);
        }
        
        if (lastSendTime == null) {
            return 0;
        }
        
        try {
            long lastTime = Long.parseLong(lastSendTime.toString());
            long currentTime = System.currentTimeMillis();
            long elapsedSeconds = (currentTime - lastTime) / 1000;
            
            if (elapsedSeconds >= SMS_SEND_INTERVAL) {
                return 0;
            }
            
            return SMS_SEND_INTERVAL - elapsedSeconds;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void recordSmsSendTime(String phone) {
        String key = SMS_SEND_TIME_PREFIX + phone;
        long currentTime = System.currentTimeMillis();
        if (redisTemplate != null) {
            // 设置过期时间为发送间隔时间，过期后自动删除
            redisTemplate.opsForValue().set(key, String.valueOf(currentTime), SMS_SEND_INTERVAL, TimeUnit.SECONDS);
        } else {
            memoryCache.put(key, String.valueOf(currentTime));
        }
    }
} 