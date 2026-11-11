package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.auth.DualTokenResponse;
import com.learning.silveragepass.entity.RefreshToken;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.exception.InvalidTokenException;
import com.learning.silveragepass.exception.RefreshTokenExpiredException;
import com.learning.silveragepass.exception.TokenReuseDetectedException;
import com.learning.silveragepass.repository.RefreshTokenRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.security.jwt.JwtUtils;
import com.learning.silveragepass.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Refresh Token服务实现
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenServiceImpl.class);
    private static final SecureRandom secureRandom = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;


    @Override
    @Transactional
    public String createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidTokenException("用户不存在"));

        // 生成随机Token
        String rawToken = generateRandomToken();
        String tokenHash = hashToken(rawToken);

        // 计算过期时间
        Instant expiryDate = Instant.now().plusMillis(jwtUtils.getRefreshTokenExpirationMs());

        // 创建并保存RefreshToken实体
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(tokenHash);
        refreshToken.setUser(user);
        refreshToken.setExpiryDate(expiryDate);
        refreshToken.setUsed(false);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(Instant.now());

        refreshTokenRepository.save(refreshToken);

        logger.debug("为用户 {} 创建了新的Refresh Token", userId);

        return rawToken;
    }

    @Override
    @Transactional
    public DualTokenResponse validateAndRotate(String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        // 查找Token
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Refresh Token无效"));

        // 检查是否已被撤销
        if (storedToken.isRevoked()) {
            logger.warn("尝试使用已撤销的Refresh Token，用户ID: {}", storedToken.getUser().getId());
            throw new InvalidTokenException("Refresh Token已被撤销");
        }

        // 检查是否已被使用（重放攻击检测）
        if (storedToken.isUsed()) {
            // 检测到重放攻击，撤销该用户的所有Token
            Long userId = storedToken.getUser().getId();
            logSecurityEvent(userId, tokenHash);
            revokeAllUserTokens(userId);
            throw new TokenReuseDetectedException("检测到Token重放攻击，所有会话已被终止");
        }

        // 检查是否过期
        if (storedToken.getExpiryDate().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException("Refresh Token已过期");
        }

        // 标记当前Token为已使用
        storedToken.setUsed(true);
        refreshTokenRepository.save(storedToken);

        // 获取用户信息
        User user = storedToken.getUser();
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toList());

        // 生成新的Access Token
        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getUsername(), roles);

        // 生成新的Refresh Token（Token轮换）
        String newRefreshToken = createRefreshToken(user.getId());

        // 计算过期时间
        long accessTokenExpiresAt = System.currentTimeMillis() + jwtUtils.getAccessTokenExpirationMs();
        long refreshTokenExpiresAt = System.currentTimeMillis() + jwtUtils.getRefreshTokenExpirationMs();

        logger.debug("为用户 {} 完成Token轮换", user.getId());

        return DualTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .accessTokenExpiresIn(jwtUtils.getAccessTokenExpirationMs() / 1000)
                .refreshTokenExpiresIn(jwtUtils.getRefreshTokenExpirationMs() / 1000)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .userId(user.getId())
                .username(user.getUsername())
                .realName(user.getRealName())
                .phone(user.getPhone())
                .roles(roles)
                .build();
    }

    @Override
    @Transactional
    public void revokeToken(String refreshToken) {
        String tokenHash = hashToken(refreshToken);

        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            logger.debug("已撤销Refresh Token，用户ID: {}", token.getUser().getId());
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        logger.info("已撤销用户 {} 的所有Refresh Token", userId);
    }

    /**
     * 生成随机Token（32字节，Base64编码）
     */
    private String generateRandomToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * 计算Token的SHA-256哈希值
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 记录安全事件
     */
    private void logSecurityEvent(Long userId, String tokenHash) {
        logger.warn("SECURITY_EVENT: Token重放攻击检测 - userId={}, tokenHash={}, timestamp={}",
                userId, tokenHash.substring(0, 8) + "...", Instant.now());
    }
}
