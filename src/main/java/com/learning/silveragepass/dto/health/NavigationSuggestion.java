package com.learning.silveragepass.dto.health;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Navigation Suggestion DTO
 * 
 * Represents a contextual suggestion to navigate to another SeniorLink feature
 * based on health assistant conversation or diary analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NavigationSuggestion {
    
    /**
     * Type of navigation suggestion
     */
    private NavigationType type;
    
    /**
     * User-friendly title for the suggestion
     */
    private String title;
    
    /**
     * Detailed description explaining why this navigation is suggested
     */
    private String description;
    
    /**
     * Target feature identifier (for Android navigation)
     */
    private String targetFeature;
    
    /**
     * Optional parameters for the target feature
     */
    private String parameters;
    
    /**
     * Priority level (1=high, 2=medium, 3=low)
     */
    private int priority;
    
    /**
     * Navigation types
     */
    public enum NavigationType {
        MEDICATION_REMINDER("medication_reminder", "用药提醒"),
        EMERGENCY_CONTACT("emergency_contact", "紧急联系人"),
        COMMUNITY_FORUM("community_forum", "社区论坛"),
        LOCATION_SHARING("location_sharing", "位置分享"),
        MEDICATION_REVIEW("medication_review", "用药复查"),
        FAMILY_MESSAGE("family_message", "家人消息");
        
        private final String code;
        private final String displayName;
        
        NavigationType(String code, String displayName) {
            this.code = code;
            this.displayName = displayName;
        }
        
        public String getCode() {
            return code;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
