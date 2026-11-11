package com.learning.silveragepass.service;

/**
 * Service for anonymizing personally identifiable information (PII) before sending data to external APIs
 * 
 * This service detects and masks PII such as names, phone numbers, addresses, and ID numbers
 * to protect user privacy when communicating with external AI services like SiliconFlow.
 * The anonymization preserves semantic meaning while removing identifying information.
 * 
 * Key Features:
 * - Chinese name detection and masking
 * - Phone number detection and masking (Chinese mobile and landline formats)
 * - Address detection and masking (Chinese address patterns)
 * - ID number detection and masking (Chinese ID card numbers)
 * - Semantic preservation (maintains context while removing PII)
 * 
 * Design Principles:
 * - Privacy-First: Aggressive PII detection to minimize data leakage
 * - Semantic Preservation: Maintain enough context for AI to provide meaningful responses
 * - Chinese-Optimized: Pattern matching specifically for Chinese PII formats
 * - Performance: Efficient regex-based detection
 * 
 * Requirements Validated:
 * - Requirement 12.6: Anonymize PII before sending to external AI services
 * - Property 55: PII Anonymization for External APIs
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
public interface AnonymizationService {
    
    /**
     * Anonymizes PII in the given text
     * 
     * This method detects and masks various types of PII including:
     * - Chinese names (2-4 character names)
     * - Phone numbers (mobile: 13x-19x, landline: area code + number)
     * - Addresses (Chinese address patterns with province/city/district)
     * - ID numbers (18-digit Chinese ID card numbers)
     * 
     * Masking Strategy:
     * - Names: Replace with generic placeholder "某人" (someone)
     * - Phone numbers: Show first 3 and last 4 digits (e.g., 138****5678)
     * - Addresses: Keep only province/city, mask detailed address
     * - ID numbers: Show first 6 and last 4 digits (e.g., 110101********1234)
     * 
     * @param text The text to anonymize
     * @return Anonymized text with PII masked
     * @throws IllegalArgumentException if text is null
     */
    String anonymize(String text);
    
    /**
     * Checks if the given text contains any PII
     * 
     * This method performs a quick check to determine if anonymization is needed.
     * Useful for performance optimization when processing large volumes of text.
     * 
     * @param text The text to check
     * @return true if PII is detected, false otherwise
     */
    boolean containsPII(String text);
    
    /**
     * Anonymizes a phone number
     * 
     * Masks the middle digits of a phone number while preserving the first 3
     * and last 4 digits for partial identification.
     * 
     * Examples:
     * - 13812345678 -> 138****5678
     * - 010-12345678 -> 010-****5678
     * 
     * @param phoneNumber The phone number to anonymize
     * @return Anonymized phone number
     */
    String anonymizePhoneNumber(String phoneNumber);
    
    /**
     * Anonymizes an address
     * 
     * Keeps only the province and city information while masking detailed
     * street address to protect privacy.
     * 
     * Examples:
     * - 北京市朝阳区建国路88号 -> 北京市朝阳区***
     * - 上海市浦东新区世纪大道1号 -> 上海市浦东新区***
     * 
     * @param address The address to anonymize
     * @return Anonymized address
     */
    String anonymizeAddress(String address);
    
    /**
     * Anonymizes a Chinese name
     * 
     * Replaces the name with a generic placeholder to protect identity
     * while maintaining the context that a person is being referenced.
     * 
     * Examples:
     * - 张三 -> 某人
     * - 李明华 -> 某人
     * 
     * @param name The name to anonymize
     * @return Anonymized name (generic placeholder)
     */
    String anonymizeName(String name);
    
    /**
     * Anonymizes a Chinese ID card number
     * 
     * Masks the middle digits while preserving the first 6 digits (region code)
     * and last 4 digits (birth date and check digit) for partial verification.
     * 
     * Example:
     * - 110101199001011234 -> 110101********1234
     * 
     * @param idNumber The ID number to anonymize
     * @return Anonymized ID number
     */
    String anonymizeIdNumber(String idNumber);
}
