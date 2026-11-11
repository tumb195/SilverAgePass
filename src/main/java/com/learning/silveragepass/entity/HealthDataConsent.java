package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Health Data Consent Entity
 * 
 * Represents explicit consent granted by an elderly user to allow a family member
 * to access their health statistics and related information. This entity enforces
 * privacy requirements by ensuring that family members can only access data when
 * explicit consent has been granted.
 * 
 * Key Features:
 * - Tracks consent for specific permission types (e.g., HEALTH_STATISTICS)
 * - Supports consent expiration for time-limited access
 * - Allows consent revocation by the elderly user
 * - Records consent grant and revocation timestamps
 * 
 * Database Table: health_data_consents
 * 
 * Requirements: 12.4 - Role-based access control with explicit user consent
 * Property 53: Family Access Consent Requirement
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Data
@Entity
@Table(name = "health_data_consents",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"user_id", "authorized_user_id", "permission_type"})
       },
       indexes = {
           @Index(name = "idx_user_authorized", columnList = "user_id, authorized_user_id"),
           @Index(name = "idx_active_consents", columnList = "active, expires_at"),
           @Index(name = "idx_permission_type", columnList = "permission_type")
       })
@NoArgsConstructor
@AllArgsConstructor
public class HealthDataConsent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The elderly user who is granting consent (data owner)
     */
    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    /**
     * The family member who is being granted access
     */
    @NotNull
    @Column(name = "authorized_user_id", nullable = false)
    private Long authorizedUserId;
    
    /**
     * Type of permission being granted
     * Examples: "HEALTH_STATISTICS", "DIARY_ENTRIES", "CONVERSATION_HISTORY", "EMOTION_ANALYSIS"
     */
    @NotNull
    @Column(name = "permission_type", nullable = false, length = 50)
    private String permissionType;
    
    /**
     * Whether this consent is currently active
     * Can be set to false when consent is revoked
     */
    @NotNull
    @Column(name = "active", nullable = false)
    private Boolean active = true;
    
    /**
     * Optional expiration date for time-limited consent
     * If null, consent does not expire
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    /**
     * Timestamp when consent was granted
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;
    
    /**
     * Timestamp when consent was revoked (if applicable)
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    
    /**
     * Timestamp when the consent record was last updated
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Pre-persist callback to set timestamps
     */
    @PrePersist
    protected void onCreate() {
        this.grantedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
    }
    
    /**
     * Pre-update callback to update timestamp
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Check if this consent is currently valid
     * A consent is valid if it is active and not expired
     * 
     * @return true if consent is valid, false otherwise
     */
    public boolean isValid() {
        if (!active) {
            return false;
        }
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            return false;
        }
        return true;
    }
    
    /**
     * Revoke this consent
     * Sets active to false and records the revocation timestamp
     */
    public void revoke() {
        this.active = false;
        this.revokedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Convenience constructor for creating a new consent
     */
    public HealthDataConsent(Long userId, Long authorizedUserId, String permissionType) {
        this.userId = userId;
        this.authorizedUserId = authorizedUserId;
        this.permissionType = permissionType;
        this.active = true;
    }
    
    /**
     * Convenience constructor with expiration
     */
    public HealthDataConsent(Long userId, Long authorizedUserId, String permissionType, LocalDateTime expiresAt) {
        this.userId = userId;
        this.authorizedUserId = authorizedUserId;
        this.permissionType = permissionType;
        this.expiresAt = expiresAt;
        this.active = true;
    }
}
