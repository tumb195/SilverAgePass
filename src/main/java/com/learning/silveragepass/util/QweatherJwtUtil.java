package com.learning.silveragepass.util;

import io.jsonwebtoken.Jwts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * 和风天气 JWT 工具类
 * 用于生成和风天气 API 的 JWT 认证 token
 * 
 * JWT 格式要求 (根据官方文档):
 * Header: {"alg": "EdDSA", "kid": "<credential_id>"}
 * Payload: {"iss": "<credential_id>", "iat": <timestamp>, "exp": <timestamp>}
 * 
 * 注意: sub 是保留字段，目前不用于认证
 */
public class QweatherJwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(QweatherJwtUtil.class);

    /**
     * 生成和风天气 JWT Token
     *
     * @param privateKeyInput 私钥（支持 PEM 格式或纯 Base64 编码）
     * @param projectId       项目 ID（保留参数，目前未使用）
     * @param credentialId    凭证 ID (用于 kid 和 iss)
     * @return JWT Token
     */
    public static String generateToken(String privateKeyInput, String projectId, String credentialId) {
        try {
            logger.debug("Generating Qweather JWT token with credentialId: {}", credentialId);
            
            // 处理私钥格式 - 移除 PEM 头尾和换行符
            String privateKeyBase64 = privateKeyInput
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s+", "");
            
            logger.debug("Private key base64 length: {}", privateKeyBase64.length());
            
            // 解码私钥
            byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("Ed25519");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            logger.debug("Private key loaded successfully, algorithm: {}", privateKey.getAlgorithm());

            // 设置时间
            long nowSeconds = System.currentTimeMillis() / 1000;
            Date iat = new Date(nowSeconds * 1000);
            Date exp = new Date((nowSeconds + 900) * 1000); // 15分钟后过期

            logger.debug("Token iat: {}, exp: {}", nowSeconds, nowSeconds + 900);

            // 构建 JWT - 和风天气要求的格式
            // Header: {"alg": "EdDSA", "kid": "<credential_id>"}
            // Payload: {"iss": "<credential_id>", "sub": "<project_id>", "iat": <timestamp>, "exp": <timestamp>}
            String token = Jwts.builder()
                    .header()
                        .add("alg", "EdDSA")  // 显式设置 alg 在前
                        .keyId(credentialId)  // kid in header
                        .and()
                    .issuer(credentialId)     // iss in payload
                    .subject(projectId)       // sub in payload (Project ID) - 必需！
                    .issuedAt(iat)            // iat in payload
                    .expiration(exp)          // exp in payload
                    .signWith(privateKey)     // 使用 EdDSA 签名
                    .compact();

            logger.debug("JWT token generated successfully, length: {}", token.length());
            return token;

        } catch (Exception e) {
            logger.error("Failed to generate Qweather JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("生成和风天气 JWT Token 失败: " + e.getMessage(), e);
        }
    }
}
