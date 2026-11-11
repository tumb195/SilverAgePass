package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.EncryptedKeyDTO;

/**
 * Key 加密服务接口
 * 负责 API Key 的加密和解密操作，使用 AES-256-CBC 算法
 */
public interface KeyEncryptionService {

    /**
     * 使用时间种子加密 Key
     * 
     * @param plainKey 明文 Key
     * @param timestamp 时间戳（用于生成 IV）
     * @return 加密后的 Key DTO，包含加密数据和时间戳
     */
    EncryptedKeyDTO encryptWithTimestamp(String plainKey, long timestamp);

    /**
     * 解密 Key
     * 
     * @param encryptedKey Base64 编码的加密 Key
     * @param timestamp 加密时使用的时间戳
     * @return 解密后的明文 Key
     */
    String decrypt(String encryptedKey, long timestamp);

    /**
     * 验证加密 Key 是否有效
     * 
     * @param encryptedKey Base64 编码的加密 Key
     * @param timestamp 加密时使用的时间戳
     * @return 是否有效
     */
    boolean validateEncryptedKey(String encryptedKey, long timestamp);
}
