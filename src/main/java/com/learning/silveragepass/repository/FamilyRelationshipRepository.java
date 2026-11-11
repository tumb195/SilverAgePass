package com.learning.silveragepass.repository;

import com.learning.silveragepass.entity.FamilyRelationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for FamilyRelationship Entity
 * 
 * Provides data access methods for managing family relationships between
 * elderly users and their family members. Supports queries for relationship
 * verification and family member lookup.
 * 
 * Requirements: 12.4 - Role-based access control for family members
 * Property 53: Family Access Consent Requirement
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Repository
public interface FamilyRelationshipRepository extends JpaRepository<FamilyRelationship, Long> {
    
    /**
     * Find a specific relationship between an elderly user and a family member
     * 
     * @param elderUserId The elderly user's ID
     * @param familyMemberId The family member's ID
     * @return Optional containing the relationship if found
     */
    Optional<FamilyRelationship> findByElderUserIdAndFamilyMemberId(Long elderUserId, Long familyMemberId);
    
    /**
     * Find all family members for a specific elderly user
     * 
     * @param elderUserId The elderly user's ID
     * @return List of family relationships
     */
    List<FamilyRelationship> findByElderUserId(Long elderUserId);
    
    /**
     * Find all elderly users that a family member is connected to
     * 
     * @param familyMemberId The family member's ID
     * @return List of family relationships
     */
    List<FamilyRelationship> findByFamilyMemberId(Long familyMemberId);
    
    /**
     * Find all verified family members for a specific elderly user
     * 
     * @param elderUserId The elderly user's ID
     * @param verified Whether the relationship is verified
     * @return List of verified family relationships
     */
    List<FamilyRelationship> findByElderUserIdAndVerified(Long elderUserId, Boolean verified);
    
    /**
     * Check if a verified relationship exists between an elderly user and a family member
     * 
     * @param elderUserId The elderly user's ID
     * @param familyMemberId The family member's ID
     * @param verified Whether the relationship must be verified
     * @return true if a verified relationship exists, false otherwise
     */
    boolean existsByElderUserIdAndFamilyMemberIdAndVerified(Long elderUserId, Long familyMemberId, Boolean verified);
    
    /**
     * Count the number of family members for an elderly user
     * 
     * @param elderUserId The elderly user's ID
     * @return Number of family relationships
     */
    long countByElderUserId(Long elderUserId);
    
    /**
     * Count the number of verified family members for an elderly user
     * 
     * @param elderUserId The elderly user's ID
     * @param verified Whether the relationship is verified
     * @return Number of verified family relationships
     */
    long countByElderUserIdAndVerified(Long elderUserId, Boolean verified);
}
