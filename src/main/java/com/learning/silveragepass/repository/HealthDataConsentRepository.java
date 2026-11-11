package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.HealthDataConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for HealthDataConsent Entity
 * 
 * Provides data access methods for managing health data consent permissions.
 * Supports queries for consent verification, expiration checking, and
 * permission management.
 * 
 * Requirements: 12.4 - Role-based access control with explicit user consent
 * Property 53: Family Access Consent Requirement
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Repository
public interface HealthDataConsentRepository extends JpaRepository<HealthDataConsent, Long> {
    
    /**
     * Find a specific consent by user, authorized user, and permission type
     * 
     * @param userId The data owner's ID
     * @param authorizedUserId The authorized user's ID
     * @param permissionType The type of permission
     * @return Optional containing the consent if found
     */
    Optional<HealthDataConsent> findByUserIdAndAuthorizedUserIdAndPermissionType(
            Long userId, Long authorizedUserId, String permissionType);
    
    /**
     * Find all consents granted by a specific user
     * 
     * @param userId The data owner's ID
     * @return List of consents
     */
    List<HealthDataConsent> findByUserId(Long userId);
    
    /**
     * Find all consents granted to a specific user
     * 
     * @param authorizedUserId The authorized user's ID
     * @return List of consents
     */
    List<HealthDataConsent> findByAuthorizedUserId(Long authorizedUserId);
    
    /**
     * Find all active consents for a specific user and permission type
     * 
     * @param userId The data owner's ID
     * @param authorizedUserId The authorized user's ID
     * @param permissionType The type of permission
     * @param active Whether the consent is active
     * @return List of active consents
     */
    List<HealthDataConsent> findByUserIdAndAuthorizedUserIdAndPermissionTypeAndActive(
            Long userId, Long authorizedUserId, String permissionType, Boolean active);
    
    /**
     * Check if a valid consent exists
     * This query checks if a consent is active and not expired
     * 
     * @param userId The data owner's ID
     * @param authorizedUserId The authorized user's ID
     * @param permissionType The type of permission
     * @param now Current timestamp for expiration check
     * @return true if a valid consent exists, false otherwise
     */
    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM HealthDataConsent c " +
           "WHERE c.userId = :userId " +
           "AND c.authorizedUserId = :authorizedUserId " +
           "AND c.permissionType = :permissionType " +
           "AND c.active = true " +
           "AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    boolean existsValidConsent(@Param("userId") Long userId,
                               @Param("authorizedUserId") Long authorizedUserId,
                               @Param("permissionType") String permissionType,
                               @Param("now") LocalDateTime now);
    
    /**
     * Find all valid (active and not expired) consents for a user
     * 
     * @param userId The data owner's ID
     * @param now Current timestamp for expiration check
     * @return List of valid consents
     */
    @Query("SELECT c FROM HealthDataConsent c " +
           "WHERE c.userId = :userId " +
           "AND c.active = true " +
           "AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<HealthDataConsent> findValidConsentsByUserId(@Param("userId") Long userId,
                                                       @Param("now") LocalDateTime now);
    
    /**
     * Find all valid consents granted to a specific user
     * 
     * @param authorizedUserId The authorized user's ID
     * @param now Current timestamp for expiration check
     * @return List of valid consents
     */
    @Query("SELECT c FROM HealthDataConsent c " +
           "WHERE c.authorizedUserId = :authorizedUserId " +
           "AND c.active = true " +
           "AND (c.expiresAt IS NULL OR c.expiresAt > :now)")
    List<HealthDataConsent> findValidConsentsByAuthorizedUserId(@Param("authorizedUserId") Long authorizedUserId,
                                                                  @Param("now") LocalDateTime now);
    
    /**
     * Find all expired consents that are still marked as active
     * Used for cleanup tasks
     * 
     * @param now Current timestamp
     * @return List of expired consents
     */
    @Query("SELECT c FROM HealthDataConsent c " +
           "WHERE c.active = true " +
           "AND c.expiresAt IS NOT NULL " +
           "AND c.expiresAt <= :now")
    List<HealthDataConsent> findExpiredActiveConsents(@Param("now") LocalDateTime now);
    
    /**
     * Count the number of active consents for a user
     * 
     * @param userId The data owner's ID
     * @param active Whether the consent is active
     * @return Number of active consents
     */
    long countByUserIdAndActive(Long userId, Boolean active);

    /**
     * Find all consents between a user and an authorized user
     * 
     * @param userId The data owner's ID
     * @param authorizedUserId The authorized user's ID
     * @return List of consents
     */
    List<HealthDataConsent> findByUserIdAndAuthorizedUserId(Long userId, Long authorizedUserId);
}
