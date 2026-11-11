package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.entity.FamilyRelationship;
import com.learning.silveragepass.entity.HealthDataConsent;
import com.learning.silveragepass.repository.FamilyRelationshipRepository;
import com.learning.silveragepass.repository.HealthDataConsentRepository;
import com.learning.silveragepass.service.FamilyAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of FamilyAccessService
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
 * - Automatic cleanup of expired consents
 * 
 * Requirements: 12.4 - Role-based access control with explicit user consent
 * Property 53: Family Access Consent Requirement
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Service
public class FamilyAccessServiceImpl implements FamilyAccessService {
    
    private static final Logger logger = LoggerFactory.getLogger(FamilyAccessServiceImpl.class);
    
    private static final String PERMISSION_HEALTH_STATISTICS = "HEALTH_STATISTICS";
    
    @Autowired
    private FamilyRelationshipRepository familyRelationshipRepository;
    
    @Autowired
    private HealthDataConsentRepository healthDataConsentRepository;
    
    // ==================== Family Relationship Management ====================
    
    @Override
    @Transactional
    public FamilyRelationship createFamilyRelationship(Long elderUserId, Long familyMemberId, String relationshipType) {
        logger.info("Creating family relationship: elder={}, family={}, type={}", 
                elderUserId, familyMemberId, relationshipType);
        
        // Check if relationship already exists
        Optional<FamilyRelationship> existing = familyRelationshipRepository
                .findByElderUserIdAndFamilyMemberId(elderUserId, familyMemberId);
        
        if (existing.isPresent()) {
            logger.warn("Family relationship already exists: elder={}, family={}", 
                    elderUserId, familyMemberId);
            return existing.get();
        }
        
        // Create new relationship
        FamilyRelationship relationship = new FamilyRelationship(elderUserId, familyMemberId, relationshipType);
        FamilyRelationship saved = familyRelationshipRepository.save(relationship);
        
        logger.info("Created family relationship with ID: {}", saved.getId());
        return saved;
    }
    
    @Override
    @Transactional
    public FamilyRelationship verifyFamilyRelationship(Long relationshipId) {
        logger.info("Verifying family relationship: {}", relationshipId);
        
        FamilyRelationship relationship = familyRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("Family relationship not found: " + relationshipId));
        
        if (relationship.getVerified()) {
            logger.warn("Family relationship already verified: {}", relationshipId);
            return relationship;
        }
        
        relationship.setVerified(true);
        relationship.setVerifiedAt(LocalDateTime.now());
        FamilyRelationship saved = familyRelationshipRepository.save(relationship);
        
        logger.info("Verified family relationship: elder={}, family={}", 
                saved.getElderUserId(), saved.getFamilyMemberId());
        return saved;
    }
    
    @Override
    public Optional<FamilyRelationship> getFamilyRelationship(Long elderUserId, Long familyMemberId) {
        return familyRelationshipRepository.findByElderUserIdAndFamilyMemberId(elderUserId, familyMemberId);
    }
    
    @Override
    public List<FamilyRelationship> getFamilyMembers(Long elderUserId) {
        return familyRelationshipRepository.findByElderUserId(elderUserId);
    }
    
    @Override
    public List<FamilyRelationship> getVerifiedFamilyMembers(Long elderUserId) {
        return familyRelationshipRepository.findByElderUserIdAndVerified(elderUserId, true);
    }
    
    @Override
    public boolean isVerifiedFamilyMember(Long elderUserId, Long familyMemberId) {
        return familyRelationshipRepository.existsByElderUserIdAndFamilyMemberIdAndVerified(
                elderUserId, familyMemberId, true);
    }
    
    @Override
    @Transactional
    public void deleteFamilyRelationship(Long relationshipId) {
        logger.info("Deleting family relationship: {}", relationshipId);
        
        FamilyRelationship relationship = familyRelationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("Family relationship not found: " + relationshipId));
        
        // Also revoke all consents associated with this relationship
        Long elderUserId = relationship.getElderUserId();
        Long familyMemberId = relationship.getFamilyMemberId();
        
        List<HealthDataConsent> consents = healthDataConsentRepository
                .findByUserIdAndAuthorizedUserId(elderUserId, familyMemberId);
        
        for (HealthDataConsent consent : consents) {
            if (consent.getActive()) {
                consent.revoke();
                healthDataConsentRepository.save(consent);
                logger.info("Revoked consent {} due to relationship deletion", consent.getId());
            }
        }
        
        familyRelationshipRepository.deleteById(relationshipId);
        logger.info("Deleted family relationship and revoked {} consents", consents.size());
    }
    
    // ==================== Health Data Consent Management ====================
    
    @Override
    @Transactional
    public HealthDataConsent grantConsent(Long userId, Long authorizedUserId, String permissionType) {
        return grantConsent(userId, authorizedUserId, permissionType, null);
    }
    
    @Override
    @Transactional
    public HealthDataConsent grantConsent(Long userId, Long authorizedUserId, String permissionType, LocalDateTime expiresAt) {
        logger.info("Granting consent: user={}, authorized={}, permission={}, expires={}", 
                userId, authorizedUserId, permissionType, expiresAt);
        
        // Check if consent already exists
        Optional<HealthDataConsent> existing = healthDataConsentRepository
                .findByUserIdAndAuthorizedUserIdAndPermissionType(userId, authorizedUserId, permissionType);
        
        if (existing.isPresent()) {
            HealthDataConsent consent = existing.get();
            if (consent.getActive()) {
                logger.warn("Active consent already exists: user={}, authorized={}, permission={}", 
                        userId, authorizedUserId, permissionType);
                // Update expiration if provided
                if (expiresAt != null) {
                    consent.setExpiresAt(expiresAt);
                    consent = healthDataConsentRepository.save(consent);
                    logger.info("Updated consent expiration: {}", consent.getId());
                }
                return consent;
            } else {
                // Reactivate existing consent
                consent.setActive(true);
                consent.setExpiresAt(expiresAt);
                consent.setRevokedAt(null);
                consent = healthDataConsentRepository.save(consent);
                logger.info("Reactivated consent: {}", consent.getId());
                return consent;
            }
        }
        
        // Create new consent
        HealthDataConsent consent = new HealthDataConsent(userId, authorizedUserId, permissionType, expiresAt);
        HealthDataConsent saved = healthDataConsentRepository.save(consent);
        
        logger.info("Created consent with ID: {}", saved.getId());
        return saved;
    }
    
    @Override
    @Transactional
    public HealthDataConsent revokeConsent(Long consentId) {
        logger.info("Revoking consent: {}", consentId);
        
        HealthDataConsent consent = healthDataConsentRepository.findById(consentId)
                .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + consentId));
        
        if (!consent.getActive()) {
            logger.warn("Consent already revoked: {}", consentId);
            return consent;
        }
        
        consent.revoke();
        HealthDataConsent saved = healthDataConsentRepository.save(consent);
        
        logger.info("Revoked consent: user={}, authorized={}, permission={}", 
                saved.getUserId(), saved.getAuthorizedUserId(), saved.getPermissionType());
        return saved;
    }
    
    @Override
    @Transactional
    public void revokeConsentByType(Long userId, Long authorizedUserId, String permissionType) {
        logger.info("Revoking consent by type: user={}, authorized={}, permission={}", 
                userId, authorizedUserId, permissionType);
        
        Optional<HealthDataConsent> consent = healthDataConsentRepository
                .findByUserIdAndAuthorizedUserIdAndPermissionType(userId, authorizedUserId, permissionType);
        
        if (consent.isPresent() && consent.get().getActive()) {
            revokeConsent(consent.get().getId());
        } else {
            logger.warn("No active consent found to revoke: user={}, authorized={}, permission={}", 
                    userId, authorizedUserId, permissionType);
        }
    }
    
    @Override
    public Optional<HealthDataConsent> getConsent(Long userId, Long authorizedUserId, String permissionType) {
        return healthDataConsentRepository.findByUserIdAndAuthorizedUserIdAndPermissionType(
                userId, authorizedUserId, permissionType);
    }
    
    @Override
    public List<HealthDataConsent> getConsentsByUser(Long userId) {
        return healthDataConsentRepository.findByUserId(userId);
    }
    
    @Override
    public List<HealthDataConsent> getValidConsentsByUser(Long userId) {
        return healthDataConsentRepository.findValidConsentsByUserId(userId, LocalDateTime.now());
    }
    
    @Override
    public List<HealthDataConsent> getConsentsByAuthorizedUser(Long authorizedUserId) {
        return healthDataConsentRepository.findByAuthorizedUserId(authorizedUserId);
    }
    
    @Override
    public List<HealthDataConsent> getValidConsentsByAuthorizedUser(Long authorizedUserId) {
        return healthDataConsentRepository.findValidConsentsByAuthorizedUserId(authorizedUserId, LocalDateTime.now());
    }
    
    @Override
    public boolean hasValidConsent(Long userId, Long authorizedUserId, String permissionType) {
        return healthDataConsentRepository.existsValidConsent(
                userId, authorizedUserId, permissionType, LocalDateTime.now());
    }
    
    // ==================== Authorization Checks ====================
    
    @Override
    public boolean isAuthorizedForHealthStatistics(Long elderUserId, Long familyMemberId) {
        return isAuthorized(elderUserId, familyMemberId, PERMISSION_HEALTH_STATISTICS);
    }
    
    @Override
    public boolean isAuthorized(Long elderUserId, Long familyMemberId, String permissionType) {
        logger.debug("Checking authorization: elder={}, family={}, permission={}", 
                elderUserId, familyMemberId, permissionType);
        
        // Check 1: Verified family relationship must exist
        boolean isVerifiedFamily = isVerifiedFamilyMember(elderUserId, familyMemberId);
        if (!isVerifiedFamily) {
            logger.debug("Authorization denied: No verified family relationship exists");
            return false;
        }
        
        // Check 2: Valid consent must exist
        boolean hasConsent = hasValidConsent(elderUserId, familyMemberId, permissionType);
        if (!hasConsent) {
            logger.debug("Authorization denied: No valid consent exists");
            return false;
        }
        
        logger.debug("Authorization granted: elder={}, family={}, permission={}", 
                elderUserId, familyMemberId, permissionType);
        return true;
    }
    
    // ==================== Maintenance Tasks ====================
    
    @Override
    @Transactional
    public int cleanupExpiredConsents() {
        logger.info("Starting cleanup of expired consents");
        
        List<HealthDataConsent> expiredConsents = healthDataConsentRepository
                .findExpiredActiveConsents(LocalDateTime.now());
        
        int count = 0;
        for (HealthDataConsent consent : expiredConsents) {
            consent.revoke();
            healthDataConsentRepository.save(consent);
            count++;
            logger.debug("Deactivated expired consent: {}", consent.getId());
        }
        
        logger.info("Cleanup complete: deactivated {} expired consents", count);
        return count;
    }
}
