package com.learning.silveragepass.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Family Relationship Entity
 * 
 * Represents the relationship between an elderly user and their family members
 * in the SeniorLink system. This entity is used to manage family connections
 * and determine who can access health-related information.
 * 
 * Key Features:
 * - Links elderly users with their family members
 * - Tracks relationship type (child, spouse, sibling, etc.)
 * - Maintains verification status for security
 * - Records creation and update timestamps
 * 
 * Database Table: family_relationships
 * 
 * Requirements: 12.4 - Role-based access control for family members
 * Property 53: Family Access Consent Requirement
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Data
@Entity
@Table(name = "family_relationships",
       uniqueConstraints = {
           @UniqueConstraint(columnNames = {"elder_user_id", "family_member_id"})
       },
       indexes = {
           @Index(name = "idx_elder_user", columnList = "elder_user_id"),
           @Index(name = "idx_family_member", columnList = "family_member_id"),
           @Index(name = "idx_verified", columnList = "verified")
       })
@NoArgsConstructor
@AllArgsConstructor
public class FamilyRelationship {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * The elderly user (primary user being cared for)
     */
    @NotNull
    @Column(name = "elder_user_id", nullable = false)
    private Long elderUserId;
    
    /**
     * The family member who has access to the elderly user's information
     */
    @NotNull
    @Column(name = "family_member_id", nullable = false)
    private Long familyMemberId;
    
    /**
     * Type of relationship (e.g., "child", "spouse", "sibling", "other")
     */
    @NotNull
    @Column(name = "relationship_type", nullable = false, length = 50)
    private String relationshipType;
    
    /**
     * Whether this relationship has been verified
     * Verification ensures the relationship is legitimate before granting access
     */
    @NotNull
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;
    
    /**
     * Timestamp when the relationship was created
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when the relationship was last updated
     */
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    /**
     * Timestamp when the relationship was verified
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;
    
    /**
     * Pre-persist callback to set timestamps
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.verified == null) {
            this.verified = false;
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
     * Convenience constructor for creating a new relationship
     */
    public FamilyRelationship(Long elderUserId, Long familyMemberId, String relationshipType) {
        this.elderUserId = elderUserId;
        this.familyMemberId = familyMemberId;
        this.relationshipType = relationshipType;
        this.verified = false;
    }
}
