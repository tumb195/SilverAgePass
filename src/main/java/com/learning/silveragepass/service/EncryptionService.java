package com.learning.silveragepass.service;

/**
 * 通用加密服务接口
 * 负责敏感数据的加密和解密操作，使用 AES-256-GCM 算法
 * 
 * 用于加密健康对话消息等敏感内容
 */
public interface EncryptionService {

    /**
     * 加密字符串
     * 
     * @param plainText 明文
     * @return 加密后的 Base64 编码字符串（包含 IV）
     * @throws com.learning.silveragepass.exception.EncryptionException 加密失败时抛出
     */
    String encrypt(String plainText);

    /**
     * 解密字符串
     * 
     * @param encryptedText Base64 编码的加密字符串（包含 IV）
     * @return 解密后的明文
     * @throws com.learning.silveragepass.exception.EncryptionException 解密失败时抛出
     */
    String decrypt(String encryptedText);

    /**
     * 验证加密文本是否有效
     * 
     * @param encryptedText Base64 编码的加密字符串
     * @return 是否有效
     */
    boolean validateEncryptedText(String encryptedText);
}
