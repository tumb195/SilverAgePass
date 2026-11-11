package com.learning.silveragepass.service;

import com.learning.silveragepass.entity.EmotionAnalysis;
import com.learning.silveragepass.entity.HealthDiaryEntry;

/**
 * Service interface for AI-powered emotion analysis
 * 
 * This service analyzes emotional content from user messages and diary entries
 * to assess psychological well-being and identify risk factors. It uses AI to
 * detect emotion types, intensity, and generate personalized suggestions for
 * elderly users.
 * 
 * Key Capabilities:
 * - Analyze text messages for emotional content
 * - Analyze diary entries for patterns and triggers
 * - Calculate psychological risk levels (0-3)
 * - Generate elderly-appropriate improvement suggestions
 * - Consider age-specific emotional patterns
 * 
 * Risk Level Classification:
 * - Level 0: Emotionally stable, no concerns
 * - Level 1: Mild concern (minor worry, slight sadness)
 * - Level 2: Moderate concern (persistent anxiety, loneliness, health fears)
 * - Level 3: High risk (hopelessness, isolation, self-harm ideation)
 * 
 * Requirements Validated:
 * - Requirement 3.1: Determine primary emotion type
 * - Requirement 3.2: Calculate emotion intensity score (0-100)
 * - Requirement 3.3: Classify emotion as positive or negative
 * - Requirement 3.4: Assess psychological risk level (0-3)
 * - Requirement 3.5: Extract emotion-related keywords
 * - Requirement 3.6: Generate personalized suggestions for risk level ≥ 2
 * - Requirement 3.8: Consider age-specific emotional patterns
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
public interface EmotionAnalysisService {
    
    /**
     * Analyzes emotional content in a user message
     * 
     * Uses AI to detect emotion type, intensity, and risk level from text.
     * Considers elderly-specific emotional patterns such as loneliness,
     * health anxiety, and family concerns.
     * 
     * @param messageContent The text content to analyze
     * @return EmotionAnalysis entity with complete analysis results
     * @throws IllegalArgumentException if messageContent is null or empty
     */
    EmotionAnalysis analyzeMessage(String messageContent);
    
    /**
     * Analyzes emotional content in a health diary entry
     * 
     * Performs comprehensive analysis including emotion triggers, sleep
     * quality assessment, and stress level evaluation. Considers both
     * the diary text content and the numerical scores.
     * 
     * @param entry The health diary entry to analyze
     * @return EmotionAnalysis entity with complete analysis results
     * @throws IllegalArgumentException if entry is null
     */
    EmotionAnalysis analyzeDiaryEntry(HealthDiaryEntry entry);
    
    /**
     * Calculates psychological risk level based on emotion analysis
     * 
     * Risk assessment considers:
     * - Primary emotion type (e.g., hopelessness, severe isolation)
     * - Emotion intensity score
     * - Presence of high-risk keywords
     * - Age-specific risk factors for elderly users
     * 
     * @param primaryEmotion The detected primary emotion
     * @param intensity The emotion intensity score (0-100)
     * @param keywords List of emotion-related keywords
     * @return Risk level (0-3)
     */
    int calculateRiskLevel(String primaryEmotion, int intensity, java.util.List<String> keywords);
    
    /**
     * Generates personalized improvement suggestions
     * 
     * Creates elderly-appropriate suggestions based on:
     * - Detected emotion type and intensity
     * - Risk level assessment
     * - User profile data (age, health conditions)
     * - Cultural context for Chinese elderly users
     * 
     * Suggestions use simple language, warm tone, and practical advice
     * suitable for senior citizens.
     * 
     * @param analysis The emotion analysis results
     * @param userId The user ID for personalization (optional)
     * @return List of personalized suggestions
     */
    java.util.List<String> generateSuggestions(EmotionAnalysis analysis, Long userId);
}
