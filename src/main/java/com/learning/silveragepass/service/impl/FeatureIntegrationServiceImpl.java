package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.dto.health.NavigationSuggestion;
import com.learning.silveragepass.dto.health.NavigationSuggestion.NavigationType;
import com.learning.silveragepass.entity.EmergencyContact;
import com.learning.silveragepass.entity.HealthDiaryEntry;
import com.learning.silveragepass.entity.User;
import com.learning.silveragepass.repository.EmergencyContactRepository;
import com.learning.silveragepass.repository.HealthDiaryEntryRepository;
import com.learning.silveragepass.repository.MedicationReminderRepository;
import com.learning.silveragepass.repository.UserRepository;
import com.learning.silveragepass.service.FeatureIntegrationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Feature Integration Service Implementation
 * 
 * Provides contextual integration between AI Health Assistant
 * and existing SeniorLink features.
 * 
 * Requirements: 11.1, 11.2, 11.3, 11.4, 11.5, 11.7, 11.8
 */
@Service
public class FeatureIntegrationServiceImpl implements FeatureIntegrationService {
    
    private static final Logger logger = LoggerFactory.getLogger(FeatureIntegrationServiceImpl.class);
    
    @Autowired
    private EmergencyContactRepository emergencyContactRepository;
    
    @Autowired
    private MedicationReminderRepository medicationReminderRepository;
    
    @Autowired
    private HealthDiaryEntryRepository healthDiaryEntryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    // Keyword patterns for contextual detection
    private static final Pattern MEDICATION_PATTERN = Pattern.compile(
        ".*(药|吃药|服药|用药|药物|药品|处方|medication|medicine|pill).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LONELINESS_PATTERN = Pattern.compile(
        ".*(孤独|寂寞|孤单|没人|无聊|lonely|alone|bored).*",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LOCATION_PATTERN = Pattern.compile(
        ".*(出门|外出|出去|散步|走路|去|visit|going out|walk|travel).*",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public List<NavigationSuggestion> analyzeForNavigationOffers(String messageContent, Long userId) {
        List<NavigationSuggestion> suggestions = new ArrayList<>();
        
        if (messageContent == null || messageContent.trim().isEmpty()) {
            return suggestions;
        }
        
        String content = messageContent.toLowerCase();
        
        // Check for medication keywords
        if (MEDICATION_PATTERN.matcher(content).matches()) {
            NavigationSuggestion medicationSuggestion = NavigationSuggestion.builder()
                .type(NavigationType.MEDICATION_REMINDER)
                .title("查看用药提醒")
                .description("您提到了用药相关的内容，要查看您的用药提醒吗？")
                .targetFeature("medication_reminder")
                .priority(1)
                .build();
            suggestions.add(medicationSuggestion);
            logger.debug("Detected medication keywords for user {}", userId);
        }
        
        // Check for loneliness keywords
        if (LONELINESS_PATTERN.matcher(content).matches()) {
            NavigationSuggestion communitySuggestion = NavigationSuggestion.builder()
                .type(NavigationType.COMMUNITY_FORUM)
                .title("访问社区论坛")
                .description("感到孤独时，可以去社区论坛和其他朋友聊聊天")
                .targetFeature("community_forum")
                .priority(1)
                .build();
            suggestions.add(communitySuggestion);
            
            NavigationSuggestion familySuggestion = NavigationSuggestion.builder()
                .type(NavigationType.FAMILY_MESSAGE)
                .title("联系家人")
                .description("也可以给家人发个消息，他们会很高兴听到您的声音")
                .targetFeature("family_message")
                .priority(2)
                .build();
            suggestions.add(familySuggestion);
            
            logger.debug("Detected loneliness keywords for user {}", userId);
        }
        
        // Check for location/travel keywords
        if (LOCATION_PATTERN.matcher(content).matches()) {
            NavigationSuggestion locationSuggestion = NavigationSuggestion.builder()
                .type(NavigationType.LOCATION_SHARING)
                .title("开启位置分享")
                .description("外出时可以开启位置分享，让家人更放心")
                .targetFeature("location_sharing")
                .priority(2)
                .build();
            suggestions.add(locationSuggestion);
            logger.debug("Detected location keywords for user {}", userId);
        }
        
        return suggestions;
    }
    
    @Override
    public NavigationSuggestion generateEmergencyContactSuggestion(Long userId, int riskLevel) {
        // Only suggest for risk level >= 2
        if (riskLevel < 2) {
            return null;
        }
        
        // Check if user has emergency contact configured
        Optional<EmergencyContact> emergencyContact = emergencyContactRepository.findByUserId(userId);
        
        if (emergencyContact.isPresent()) {
            EmergencyContact contact = emergencyContact.get();
            String contactName = contact.getRemark() != null && !contact.getRemark().isEmpty()
                ? contact.getRemark()
                : "紧急联系人";
            
            NavigationSuggestion suggestion = NavigationSuggestion.builder()
                .type(NavigationType.EMERGENCY_CONTACT)
                .title("联系" + contactName)
                .description(riskLevel >= 3 
                    ? "如果感到非常难过，请立即联系您的紧急联系人或拨打心理援助热线"
                    : "感到不舒服时，可以联系您的紧急联系人寻求帮助")
                .targetFeature("emergency_contact")
                .parameters("contactId=" + contact.getId())
                .priority(1)
                .build();
            
            logger.info("Generated emergency contact suggestion for user {} with risk level {}", 
                userId, riskLevel);
            return suggestion;
        } else {
            // Suggest adding emergency contact
            NavigationSuggestion suggestion = NavigationSuggestion.builder()
                .type(NavigationType.EMERGENCY_CONTACT)
                .title("添加紧急联系人")
                .description("建议您添加一个紧急联系人，以便在需要时能够快速联系")
                .targetFeature("emergency_contact_add")
                .priority(2)
                .build();
            
            logger.info("Suggested adding emergency contact for user {} with risk level {}", 
                userId, riskLevel);
            return suggestion;
        }
    }
    
    @Override
    public NavigationSuggestion checkMedicationScheduleImpact(Long userId) {
        // Check last 3 diary entries for sleep quality issues
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(3);
        
        List<HealthDiaryEntry> recentEntries = healthDiaryEntryRepository
            .findByUserIdAndEntryDateBetweenOrderByEntryDateDesc(userId, startDate, endDate);
        
        if (recentEntries.size() < 3) {
            return null; // Not enough data
        }
        
        // Check if all recent entries have poor sleep quality (<=2)
        boolean hasSleepIssues = recentEntries.stream()
            .allMatch(entry -> entry.getSleepQuality() <= 2);
        
        if (!hasSleepIssues) {
            return null;
        }
        
        // Check if user has medication reminders
        boolean hasMedications = !medicationReminderRepository
            .findByUserIdAndActiveTrue(userId).isEmpty();
        
        if (hasMedications) {
            NavigationSuggestion suggestion = NavigationSuggestion.builder()
                .type(NavigationType.MEDICATION_REVIEW)
                .title("检查用药时间")
                .description("您最近睡眠质量不佳，建议检查用药时间是否影响睡眠")
                .targetFeature("medication_reminder")
                .priority(1)
                .build();
            
            logger.info("Detected sleep issues with medication for user {}", userId);
            return suggestion;
        }
        
        return null;
    }
    
    @Override
    public String getWeatherDataForDate(Long userId, LocalDate date) {
        // TODO: Integrate with existing weather service
        // For now, return placeholder
        // In full implementation, this would call the weather service
        // to get historical weather data for the specified date
        
        logger.debug("Weather data requested for user {} on date {}", userId, date);
        
        // Placeholder implementation
        return null;
    }
    
    @Override
    public String getUserProfileContext(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);
        
        if (!userOpt.isPresent()) {
            logger.warn("User not found: {}", userId);
            return null;
        }
        
        User user = userOpt.get();
        StringBuilder context = new StringBuilder();
        
        // Add age information if available
        if (user.getBirthday() != null) {
            int age = calculateAge(user.getBirthday());
            context.append("年龄：").append(age).append("岁");
        }
        
        // Add gender information if available
        if (user.getGender() != null) {
            if (context.length() > 0) {
                context.append("，");
            }
            context.append("性别：").append(user.getGender());
        }
        
        // TODO: Add health conditions if available in user profile
        // This would require additional fields in the User entity
        
        String profileContext = context.length() > 0 ? context.toString() : null;
        logger.debug("Generated profile context for user {}: {}", userId, profileContext);
        
        return profileContext;
    }
    
    /**
     * Calculate age from birthday
     */
    private int calculateAge(LocalDate birthday) {
        if (birthday == null) {
            return 0;
        }
        
        LocalDate currentDate = LocalDate.now();
        return Period.between(birthday, currentDate).getYears();
    }
}
