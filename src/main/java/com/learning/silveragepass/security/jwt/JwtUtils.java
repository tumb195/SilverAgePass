package com.learning.silveragepass.security.jwt;

import com.learning.silveragepass.exception.*;
import com.learning.silveragepass.security.UserPrincipal;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationMs;

    @Value("${app.jwt.access-token-expiration:1800000}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration:604800000}")
    private long refreshTokenExpirationMs;

    // ==================== 原有方法（保持向后兼容） ====================

    public String generateJwtToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        return Jwts.builder()
                .setSubject(userPrincipal.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateJwtToken(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }


    private Key key() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String getUserNameFromJwtToken(String token) {
        return Jwts.parser().verifyWith((SecretKey) key()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }

    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parser().verifyWith((SecretKey) key()).build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        }

        return false;
    }

    // ==================== 双Token认证新方法 ====================

    /**
     * 生成Access Token（包含完整claims）
     */
    public String generateAccessToken(Long userId, String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("tokenType", TOKEN_TYPE_ACCESS)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 从Authentication生成Access Token
     */
    public String generateAccessToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return generateAccessToken(userPrincipal.getId(), userPrincipal.getUsername(), roles);
    }

    /**
     * 从UserPrincipal生成Access Token
     */
    public String generateAccessToken(UserPrincipal userPrincipal) {
        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return generateAccessToken(userPrincipal.getId(), userPrincipal.getUsername(), roles);
    }

    /**
     * 获取Access Token过期时间（毫秒）
     */
    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    /**
     * 获取Refresh Token过期时间（毫秒）
     */
    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    /**
     * 验证Token并返回详细结果
     */
    public TokenValidationResult validateTokenWithDetails(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return TokenValidationResult.valid(claims);
        } catch (ExpiredJwtException e) {
            logger.warn("Token已过期: {}", e.getMessage());
            String tokenType = getTokenTypeFromExpiredToken(e);
            if (TOKEN_TYPE_REFRESH.equals(tokenType)) {
                return TokenValidationResult.error("REFRESH_TOKEN_EXPIRED", "Refresh Token已过期");
            }
            return TokenValidationResult.error("ACCESS_TOKEN_EXPIRED", "Access Token已过期");
        } catch (SignatureException e) {
            logger.warn("Token签名无效: {}", e.getMessage());
            return TokenValidationResult.error("INVALID_TOKEN", "Token签名无效");
        } catch (MalformedJwtException e) {
            logger.warn("Token格式错误: {}", e.getMessage());
            return TokenValidationResult.error("MALFORMED_TOKEN", "Token格式错误");
        } catch (UnsupportedJwtException e) {
            logger.warn("不支持的Token: {}", e.getMessage());
            return TokenValidationResult.error("MALFORMED_TOKEN", "不支持的Token格式");
        } catch (IllegalArgumentException e) {
            logger.warn("Token为空: {}", e.getMessage());
            return TokenValidationResult.error("MALFORMED_TOKEN", "Token不能为空");
        }
    }

    /**
     * 从过期的Token中获取tokenType
     */
    private String getTokenTypeFromExpiredToken(ExpiredJwtException e) {
        try {
            Claims claims = e.getClaims();
            if (claims != null) {
                return (String) claims.get("tokenType");
            }
        } catch (Exception ex) {
            logger.debug("无法从过期Token中获取tokenType");
        }
        return TOKEN_TYPE_ACCESS;
    }

    /**
     * 从Token中提取所有Claims（不验证签名，仅用于错误处理）
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 从Token中获取角色列表
     */
    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }

    /**
     * 从Token中获取Token类型
     */
    public String getTokenTypeFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("tokenType", String.class);
    }

    /**
     * 根据验证结果抛出对应异常
     */
    public void throwExceptionForValidationResult(TokenValidationResult result) {
        if (result.isValid()) {
            return;
        }

        switch (result.getErrorCode()) {
            case "ACCESS_TOKEN_EXPIRED":
                throw new AccessTokenExpiredException(result.getErrorMessage());
            case "REFRESH_TOKEN_EXPIRED":
                throw new RefreshTokenExpiredException(result.getErrorMessage());
            case "INVALID_TOKEN":
                throw new InvalidTokenException(result.getErrorMessage());
            case "MALFORMED_TOKEN":
                throw new MalformedTokenException(result.getErrorMessage());
            default:
                throw new InvalidTokenException(result.getErrorMessage());
        }
    }

    // ==================== Token验证结果类 ====================

    /**
     * Token验证结果
     */
    @Getter
    public static class TokenValidationResult {
        private final boolean valid;
        private final Claims claims;
        private final String errorCode;
        private final String errorMessage;

        private TokenValidationResult(boolean valid, Claims claims, String errorCode, String errorMessage) {
            this.valid = valid;
            this.claims = claims;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public static TokenValidationResult valid(Claims claims) {
            return new TokenValidationResult(true, claims, null, null);
        }

        public static TokenValidationResult error(String errorCode, String errorMessage) {
            return new TokenValidationResult(false, null, errorCode, errorMessage);
        }
    }
}
