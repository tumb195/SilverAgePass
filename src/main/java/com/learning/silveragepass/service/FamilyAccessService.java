package com.learning.silveragepass.service;

import com.learning.silveragepass.entity.FamilyRelationship;
import com.learning.silveragepass.entity.HealthDataConsent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service Interface for Family Access Control
 * 
 * Provides business logic for managing family relationships and health data
 * consent permissions. This service enforces privacy requirements by ensuring
 * that family members can only access elderly users' health data when both
 * a verified family relationship exists and explicit consent has been granted.
 * 
 * Key Features:
 * - Verify family relationships between users
 * - Manage health data consent permissions
 * - Check authorization for data access
 * - Support consent expiration and revocation
 * 
 * Requirements: 12.4 - Role-based access control with explicit user consent
 * Property 53: Family Access Consent Requirement
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
public interface FamilyAccessService {
    
    // ==================== Family Relationship Management ====================
    
    /**
     * Create a new family relationship
     * 
     * @param elderUserId The elderly user's ID
     * @param familyMemberId The family member's ID
     * @param relationshipType The type of relationship (e.g., "child", "spouse")
     * @return The created family relationship
     */
    FamilyRelationship createFamilyRelationship(Long elderUserId, Long familyMemberId, String relationshipType);
    
    /**
     * Verify a family relationship
     * This should be called after proper verification procedures (e.g., ID verification, video call)
     * 
     * @param relationshipId The relationship ID to verify
     * @return The verified family relationship
     */
    FamilyRelationship verifyFamilyRelationship(Long relationshipId);
    
    /**
     * Get a specific family relationship
     * 
     * @param elderUserId The elderly user's ID
     * @param familyMemberId The family member's ID
     * @return Optional containing the relationship if found
     */
    Optional<FamilyRelationship> getFamilyRelationship(Long elderUserId, Long familyMemberId);
    
    /**
     * Get all family members for an elderly user
     * 
     * @param elderUserId The elderly user's ID
     * @return List of family relationships
     */
    List<FamilyRelationship> getFamilyMembers(Long elderUserId);
    
    /**
     * Get all verified family members for an elderly user
     * 
     * @param elderUserId The elderly user's ID
     * @return List of verified family relationships
     */
    List<FamilyRelationship> getVerifiedFamilyMembers(Long elderUserId);
    
    /**
     * Check if a verified family relationship exists
     * 
     * @param elderUserId The elderly user's ID
     * @param familyMemberId The family member's ID
     * @return true if a verified relationship exists, false otherwise
     */
    boolean isVerifiedFamilyMember(Long elderUserId, Long familyMemberId);
    
    /**
     * Delete a family relationship
     * 
     * @param relationshipId The relationship ID to delete
     */
    void deleteFamilyRelationship(Long relationshipId);
    
    // ==================== Health Data Consent Management ====================
    
    /**
     * Grant health data consent to a family member
     * 
     * @param userId The data owner's ID (elderly user)
     * @param authorizedUserId The authorized user's ID (family member)
     * @param permissionType The type of permission (e.g., "HEALTH_STATISTICS")
     * @return The created consent
     */
    HealthDataConsent grantConsent(Long userId, Long authorizedUserId, String permissionType);
    
    /**
     * Grant health data consent with expiration
     * 
     * @param userId The data owner's ID (elderly user)
     * @param authorizedUserId The authorized user's ID (family member)
     * @param permissionType The type of permission
     * @param expiresAt The expiration timestamp
     * @return The created consent
     */
    HealthDataConsent grantConsent(Long userId, Long authorizedUserId, String permissionType, LocalDateTime expiresAt);
    
    /**
     * Revoke health data consent
     * 
     * @param consentId The consent ID to revoke
     * @return The revoked consent
     */
    HealthDataConsent revokeConsent(Long consentId);
    
    /**
     * Revoke all consents for a specific permission type
     * 
     * @param userId The data owner's ID
     * @param authorizedUserId The authorized user's ID
     * @param permissionType The type of permission
     */
    void revokeConsentByType(Long userId, Long authorizedUserId, String permissionType);
    
    /**
     * Get a specific consent
     * 
     * @param userId The data owner's ID
     * @param authorizedUserId The authorized user's ID
     * @param permissionType The type of permission
     * @return Optional containing the consent if found
     */
    Optional<HealthDataConsent> getConsent(Long userId, Long authorizedUserId, String permissionType);
    
    /**
     * Get all consents granted by a user
     * 
     * @param userId The data owner's ID
     * @return List of consents
     */
    List<HealthDataConsent> getConsentsByUser(Long userId);
    
    /**
     * Get all valid (active and not expired) consents granted by a user
     * 
     * @param userId The data owner's ID
     * @return List of valid consents
     */
    List<HealthDataConsent> getValidConsentsByUser(Long userId);
    
    /**
     * Get all consents granted to a user
     * 
     * @param authorizedUserId The authorized user's ID
     * @return List of consents
     */
    List<HealthDataConsent> getConsentsByAuthorizedUser(Long authorizedUserId);
    
    /**
     * Get all valid consents granted to a user
     * 
     * @param authorizedUserId The authorized user's ID
     * @return List of valid consents
     */
    List<HealthDataConsent> getValidConsentsByAuthorizedUser(Long authorizedUserId);
    
    /**
     * Check if a valid consent exists
     * 
     * @param userId The data owner's ID
     * @param authorizedUserId The authorized user's ID
     * @param permissionType The type of permission
     * @return true if a valid consent exists, false otherwise
     */
    boolean hasValidConsent(Long userId, Long authorizedUserId, String permissionType);
    
    // ==================== Authorization Checks ====================
    
    /**
     * Check if a family member is authorized to access health statistics
     * This is the main authorization method used by controllers
     * 
     * Requirements:
     * 1. A verified family relationship must exist
     * 2. Valid consent for HEALTH_STATISTICS permission must exist
     * 
     * @param elderUserId The elderly user's ID (data owner)
     * @param familyMemberId The family member's ID (requesting access)
     * @return true if authorized, false otherwise
     */
    boolean isAuthorizedForHealthStatistics(Long elderUserId, Long familyMemberId);
    
    /**
     * Check if a family member is authorized for a specific permission type
     * 
     * @param elderUserId The elderly user's ID (data owner)
     * @param familyMemberId The family member's ID (requesting access)
     * @param permissionType The type of permission
     * @return true if authorized, false otherwise
     */
    boolean isAuthorized(Long elderUserId, Long familyMemberId, String permissionType);
    
    // ==================== Maintenance Tasks ====================
    
    /**
     * Clean up expired consents
     * This should be called periodically (e.g., daily) to deactivate expired consents
     * 
     * @return Number of consents deactivated
     */
    int cleanupExpiredConsents();
}
