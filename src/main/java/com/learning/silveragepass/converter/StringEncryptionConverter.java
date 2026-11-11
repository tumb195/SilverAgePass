package com.learning.silveragepass.converter;

import com.learning.silveragepass.exception.EncryptionException;
import com.learning.silveragepass.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA 字符串加密转换器
 * 自动加密/解密数据库中的敏感字符串字段
 * 
 * 使用 AES-256-GCM 算法确保数据在静态存储时的安全性
 */
@Converter
@Component
public class StringEncryptionConverter implements AttributeConverter<String, String> {

    private static final Logger logger = LoggerFactory.getLogger(StringEncryptionConverter.class);

    private EncryptionService encryptionService;

    /**
     * 使用 setter 注入避免循环依赖问题
     * JPA Converter 在 Spring 容器初始化早期被创建
     */
    @Autowired
    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }

        if (attribute.isEmpty()) {
            return attribute;
        }

        try {
            String encrypted = encryptionService.encrypt(attribute);
            logger.trace("Encrypted attribute for database storage");
            return encrypted;
        } catch (EncryptionException e) {
            logger.error("Failed to encrypt attribute: {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        if (dbData.isEmpty()) {
            return dbData;
        }

        try {
            String decrypted = encryptionService.decrypt(dbData);
            logger.trace("Decrypted attribute from database");
            return decrypted;
        } catch (EncryptionException e) {
            logger.error("Failed to decrypt attribute: {}", e.getMessage());
            // Return the original data if decryption fails (for backward compatibility)
            // This allows migration from unencrypted to encrypted data
            logger.warn("Returning original data due to decryption failure - possible unencrypted legacy data");
            return dbData;
        }
    }
}
