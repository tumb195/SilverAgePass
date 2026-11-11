package com.learning.silveragepass.dto;

/**
 * 加密 Key 数据传输对象
 */
public class EncryptedKeyDTO {

    /**
     * AES 加密后的 Key（Base64 编码）
     */
    private String encryptedKey;

    /**
     * 加密时间戳（用于解密）
     */
    private Long timestamp;

    /**
     * Key 有效期（秒）
     */
    private Long expiresIn;

    /**
     * 加密算法标识
     */
    private String algorithm;

    public EncryptedKeyDTO() {
    }

    public EncryptedKeyDTO(String encryptedKey, Long timestamp, Long expiresIn, String algorithm) {
        this.encryptedKey = encryptedKey;
        this.timestamp = timestamp;
        this.expiresIn = expiresIn;
        this.algorithm = algorithm;
    }

    public String getEncryptedKey() {
        return encryptedKey;
    }

    public void setEncryptedKey(String encryptedKey) {
        this.encryptedKey = encryptedKey;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
