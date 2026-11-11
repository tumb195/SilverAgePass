package com.learning.silveragepass.service;

import com.learning.silveragepass.entity.EmotionAnalysis;

/**
 * Service interface for sending intervention alerts
 * 
 * This service handles high-risk psychological alerts when emotion analysis
 * detects concerning patterns. It notifies designated family members or
 * caregivers through existing SeniorLink notification channels while
 * respecting privacy and consent requirements.
 * 
 * Key Capabilities:
 * - Send high-risk alerts (risk level = 3)
 * - Notify family members via existing channels
 * - Respect privacy settings and consent
 * - Log alerts for administrative review
 * - Handle notification failures gracefully
 * 
 * Privacy Considerations:
 * - Only shares risk level and timestamp by default
 * - Detailed message content requires explicit authorization
 * - Respects user's family access consent settings
 * 
 * Requirements Validated:
 * - Requirement 3.7: Trigger alerts when risk level = 3
 * - Requirement 11.6: Notify family members via SeniorLink channels
 * - Requirement 12.5: Alert privacy protection
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
public interface AlertService {
    
    /**
     * Sends a high-risk intervention alert to designated contacts
     * 
     * Triggers when emotion analysis detects risk level 3 (high risk).
     * Notifies family members or caregivers through existing SeniorLink
     * notification channels (push notifications, SMS, in-app messages).
     * 
     * Alert Content (Privacy-Protected):
     * - Risk level (3)
     * - Timestamp of detection
     * - General concern category (emotional distress)
     * - Suggested action (contact elderly user)
     * 
     * Detailed message content is NOT included unless user has explicitly
     * authorized family access to health data.
     * 
     * @param userId The ID of the elderly user
     * @param analysis The emotion analysis with risk level 3
     * @throws IllegalArgumentException if userId is null or analysis is null
     * @throws IllegalStateException if risk level is not 3
     */
    void sendHighRiskAlert(Long userId, EmotionAnalysis analysis);
    
    /**
     * Checks if a user has authorized family access to detailed health data
     * 
     * @param userId The ID of the elderly user
     * @return true if family members can access detailed health information
     */
    boolean hasFamilyAccessConsent(Long userId);
    
    /**
     * Gets the list of family members who should receive alerts
     * 
     * @param userId The ID of the elderly user
     * @return List of family member user IDs
     */
    java.util.List<Long> getAlertRecipients(Long userId);
}
