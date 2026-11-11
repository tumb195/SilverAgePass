package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.health.NavigationSuggestion;

import java.util.List;

/**
 * Feature Integration Service
 * 
 * Integrates AI Health Assistant with existing SeniorLink features
 * to provide contextual navigation offers and cross-feature recommendations.
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.7, 11.8
 */
public interface FeatureIntegrationService {
    
    /**
     * Analyze message content and generate contextual navigation suggestions
     * 
     * Detects keywords related to:
     * - Medication (offers navigation to medication reminders)
     * - Loneliness (recommends community forum)
     * - Location/travel (offers location sharing)
     * 
     * @param messageContent User message content
     * @param userId User ID for personalization
     * @return List of navigation suggestions
     * Requirements: 11.1, 11.3, 11.5
     */
    List<NavigationSuggestion> analyzeForNavigationOffers(String messageContent, Long userId);
    
    /**
     * Generate emergency contact suggestions based on risk level
     * 
     * For risk level >= 2, suggests contacting emergency contacts
     * configured in the user's SeniorLink profile.
     * 
     * @param userId User ID
     * @param riskLevel Emotion analysis risk level (0-3)
     * @return Navigation suggestion for emergency contacts, or null if not applicable
     * Requirements: 11.2
     */
    NavigationSuggestion generateEmergencyContactSuggestion(Long userId, int riskLevel);
    
    /**
     * Check if user has medication schedule issues affecting sleep
     * 
     * Analyzes recent diary entries for sleep quality issues and
     * suggests reviewing medication schedules if correlation found.
     * 
     * @param userId User ID
     * @return Navigation suggestion for medication review, or null if not applicable
     * Requirements: 11.4
     */
    NavigationSuggestion checkMedicationScheduleImpact(Long userId);
    
    /**
     * Get weather data for mood correlation analysis
     * 
     * Retrieves weather data from existing SeniorLink weather service
     * for use in diary analysis when weather affects mood.
     * 
     * @param userId User ID
     * @param date Date for weather data
     * @return Weather description, or null if not available
     * Requirements: 11.7
     */
    String getWeatherDataForDate(Long userId, java.time.LocalDate date);
    
    /**
     * Get user profile data for health insights
     * 
     * Retrieves user profile information (age, health conditions)
     * from SeniorLink database for personalized health insight generation.
     * 
     * @param userId User ID
     * @return User profile summary for health context
     * Requirements: 11.8
     */
    String getUserProfileContext(Long userId);
}
