package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.RefreshToken;
import com.learning.silveragepass.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Refresh Token数据访问接口
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * 根据Token哈希值查找
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 根据用户查找所有Token
     */
    List<RefreshToken> findByUser(User user);

    /**
     * 根据用户ID查找所有Token
     */
    List<RefreshToken> findByUserId(Long userId);

    /**
     * 查找用户所有未撤销的Token
     */
    List<RefreshToken> findAllByUserAndRevokedFalse(User user);

    /**
     * 查找用户所有未撤销且未过期的Token
     */
    @Query("SELECT rt FROM RefreshToken rt WHERE rt.user = :user AND rt.revoked = false AND rt.expiryDate > :now")
    List<RefreshToken> findAllValidTokensByUser(@Param("user") User user, @Param("now") Instant now);

    /**
     * 删除用户的所有Token
     */
    @Modifying
    void deleteByUser(User user);

    /**
     * 删除用户的所有Token（按用户ID）
     */
    @Modifying
    void deleteByUserId(Long userId);

    /**
     * 撤销用户的所有Token
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId")
    void revokeAllByUserId(@Param("userId") Long userId);

    /**
     * 删除所有过期的Token
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteAllExpired(@Param("now") Instant now);

    /**
     * 删除所有已撤销的Token
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.revoked = true")
    void deleteAllRevoked();
}
