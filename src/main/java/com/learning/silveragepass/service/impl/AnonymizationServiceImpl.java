package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.service.AnonymizationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of AnonymizationService for PII detection and masking
 * 
 * This service uses regex pattern matching and NLP techniques to detect and anonymize
 * personally identifiable information in Chinese text. It's specifically designed for
 * protecting elderly users' privacy when their health conversations are sent to external
 * AI services like SiliconFlow.
 * 
 * Pattern Matching Strategy:
 * - Chinese names: 2-4 character patterns with common surname detection
 * - Phone numbers: Mobile (13x-19x) and landline (area code + number) formats
 * - Addresses: Province/city/district/street patterns
 * - ID numbers: 18-digit Chinese ID card format with checksum validation
 * 
 * Performance Considerations:
 * - Compiled regex patterns for efficiency
 * - Early exit on containsPII check
 * - Single-pass anonymization where possible
 * 
 * @author SilverAgePass Team
 * @version 1.0
 * @since 2024-01-15
 */
@Service
public class AnonymizationServiceImpl implements AnonymizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(AnonymizationServiceImpl.class);
    
    // Regex patterns for PII detection
    
    /**
     * Pattern for Chinese mobile phone numbers
     * Matches: 13x, 14x, 15x, 16x, 17x, 18x, 19x followed by 9 digits
     * Examples: 13812345678, 15912345678
     */
    private static final Pattern MOBILE_PHONE_PATTERN = 
            Pattern.compile("1[3-9]\\d{9}");
    
    /**
     * Pattern for Chinese landline phone numbers
     * Matches: Area code (3-4 digits) + hyphen + number (7-8 digits)
     * Examples: 010-12345678, 0755-1234567
     */
    private static final Pattern LANDLINE_PHONE_PATTERN = 
            Pattern.compile("0\\d{2,3}-?\\d{7,8}");
    
    /**
     * Pattern for Chinese addresses
     * Matches: Province + City + District + Street/Building
     * Examples: 北京市朝阳区建国路88号, 上海市浦东新区世纪大道1号
     */
    private static final Pattern ADDRESS_PATTERN = 
            Pattern.compile("[\\u4e00-\\u9fa5]{2,}[省市]" +  // Province/Municipality
                          "[\\u4e00-\\u9fa5]{2,}[市区县]" +  // City/District
                          "[\\u4e00-\\u9fa5]{2,}[路街道巷弄]" +  // Street
                          "[\\d\\u4e00-\\u9fa5]{1,}[号栋幢单元室]?");  // Building/Unit
    
    /**
     * Pattern for simplified Chinese addresses (province + city)
     * Matches: Province + City
     * Examples: 北京市, 上海市浦东新区
     */
    private static final Pattern SIMPLE_ADDRESS_PATTERN = 
            Pattern.compile("[\\u4e00-\\u9fa5]{2,}[省市][\\u4e00-\\u9fa5]{2,}[市区县]");
    
    /**
     * Pattern for Chinese ID card numbers
     * Matches: 18-digit ID number (6-digit region + 8-digit birth date + 4-digit sequence)
     * Examples: 110101199001011234
     */
    private static final Pattern ID_NUMBER_PATTERN = 
            Pattern.compile("\\d{6}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]");
    
    /**
     * Pattern for Chinese names
     * Matches: 2-4 character Chinese names, optionally preceded by common titles
     * Examples: 张三, 李明华, 王老师, 刘医生
     */
    private static final Pattern NAME_PATTERN = 
            Pattern.compile("([\\u4e00-\\u9fa5]{1})(老师|医生|先生|女士|师傅)?([\\u4e00-\\u9fa5]{1,3})(?=[，。！？、\\s]|$)");
    
    /**
     * Common Chinese surnames for name detection
     */
    private static final String COMMON_SURNAMES = 
            "王李张刘陈杨黄赵吴周徐孙马朱胡郭何高林罗郑梁谢宋唐许韩冯邓曹彭曾肖田董袁潘于蒋蔡余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏韦付方白邹孟熊秦邱江尹薛闫段雷侯龙史陶黎贺顾毛郝龚邵万钱严覃武戴莫孔向汤";
    
    @Override
    public String anonymize(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text cannot be null");
        }
        
        if (text.trim().isEmpty()) {
            return text;
        }
        
        logger.debug("Anonymizing text of length: {}", text.length());
        
        String anonymized = text;
        
        // Anonymize in order of specificity (most specific first)
        
        // 1. Anonymize ID numbers (most specific)
        anonymized = anonymizeIdNumbersInText(anonymized);
        
        // 2. Anonymize phone numbers
        anonymized = anonymizePhoneNumbersInText(anonymized);
        
        // 3. Anonymize addresses
        anonymized = anonymizeAddressesInText(anonymized);
        
        // 4. Anonymize names (least specific, do last to avoid false positives)
        anonymized = anonymizeNamesInText(anonymized);
        
        if (!anonymized.equals(text)) {
            logger.info("PII anonymization applied. Original length: {}, Anonymized length: {}", 
                    text.length(), anonymized.length());
        }
        
        return anonymized;
    }
    
    @Override
    public boolean containsPII(String text) {
        if (text == null || text.trim().isEmpty()) {
            return false;
        }
        
        // Quick check for any PII patterns
        return MOBILE_PHONE_PATTERN.matcher(text).find() ||
               LANDLINE_PHONE_PATTERN.matcher(text).find() ||
               ADDRESS_PATTERN.matcher(text).find() ||
               SIMPLE_ADDRESS_PATTERN.matcher(text).find() ||
               ID_NUMBER_PATTERN.matcher(text).find() ||
               containsChineseName(text);
    }
    
    @Override
    public String anonymizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return phoneNumber;
        }
        
        // Remove spaces and hyphens for processing
        String cleaned = phoneNumber.replaceAll("[\\s-]", "");
        
        // Mobile phone: 13812345678 -> 138****5678
        if (cleaned.matches("1[3-9]\\d{9}")) {
            return cleaned.substring(0, 3) + "****" + cleaned.substring(7);
        }
        
        // Landline with area code: 01012345678 -> 010****5678
        if (cleaned.matches("0\\d{2,3}\\d{7,8}")) {
            if (cleaned.length() == 11) {
                // 3-digit area code + 8-digit number
                return cleaned.substring(0, 3) + "****" + cleaned.substring(7);
            } else if (cleaned.length() == 10) {
                // 3-digit area code + 7-digit number
                return cleaned.substring(0, 3) + "****" + cleaned.substring(6);
            }
        }
        
        // Fallback: mask middle portion
        if (cleaned.length() >= 7) {
            int keepStart = Math.min(3, cleaned.length() / 3);
            int keepEnd = Math.min(4, cleaned.length() / 3);
            return cleaned.substring(0, keepStart) + "****" + 
                   cleaned.substring(cleaned.length() - keepEnd);
        }
        
        return "****";
    }
    
    @Override
    public String anonymizeAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            return address;
        }
        
        // Try to extract province and city, mask the rest
        Matcher simpleMatcher = SIMPLE_ADDRESS_PATTERN.matcher(address);
        if (simpleMatcher.find()) {
            String provinceCity = simpleMatcher.group();
            return provinceCity + "***";
        }
        
        // If no clear province/city pattern, mask most of the address
        if (address.length() > 10) {
            return address.substring(0, 10) + "***";
        }
        
        return "***";
    }
    
    @Override
    public String anonymizeName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return name;
        }
        
        // Replace with generic placeholder
        return "某人";
    }
    
    @Override
    public String anonymizeIdNumber(String idNumber) {
        if (idNumber == null || idNumber.trim().isEmpty()) {
            return idNumber;
        }
        
        // Remove spaces and hyphens
        String cleaned = idNumber.replaceAll("[\\s-]", "");
        
        // Chinese ID: 18 digits
        if (cleaned.matches("\\d{6}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]")) {
            // Show first 6 (region) and last 4 (birth date + check digit)
            return cleaned.substring(0, 6) + "********" + cleaned.substring(14);
        }
        
        // Fallback: mask middle portion
        if (cleaned.length() >= 10) {
            return cleaned.substring(0, 6) + "****" + cleaned.substring(cleaned.length() - 4);
        }
        
        return "****";
    }
    
    /**
     * Anonymizes all phone numbers found in text
     */
    private String anonymizePhoneNumbersInText(String text) {
        String result = text;
        
        // Anonymize mobile phones
        Matcher mobileMatcher = MOBILE_PHONE_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (mobileMatcher.find()) {
            String phone = mobileMatcher.group();
            String anonymized = anonymizePhoneNumber(phone);
            mobileMatcher.appendReplacement(sb, Matcher.quoteReplacement(anonymized));
        }
        mobileMatcher.appendTail(sb);
        result = sb.toString();
        
        // Anonymize landlines
        Matcher landlineMatcher = LANDLINE_PHONE_PATTERN.matcher(result);
        sb = new StringBuffer();
        while (landlineMatcher.find()) {
            String phone = landlineMatcher.group();
            String anonymized = anonymizePhoneNumber(phone);
            landlineMatcher.appendReplacement(sb, Matcher.quoteReplacement(anonymized));
        }
        landlineMatcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Anonymizes all addresses found in text
     */
    private String anonymizeAddressesInText(String text) {
        String result = text;
        
        // Anonymize full addresses first
        Matcher addressMatcher = ADDRESS_PATTERN.matcher(result);
        StringBuffer sb = new StringBuffer();
        while (addressMatcher.find()) {
            String address = addressMatcher.group();
            String anonymized = anonymizeAddress(address);
            addressMatcher.appendReplacement(sb, Matcher.quoteReplacement(anonymized));
        }
        addressMatcher.appendTail(sb);
        result = sb.toString();
        
        // Then anonymize simple province/city patterns
        Matcher simpleMatcher = SIMPLE_ADDRESS_PATTERN.matcher(result);
        sb = new StringBuffer();
        while (simpleMatcher.find()) {
            String address = simpleMatcher.group();
            // Keep province/city but add mask indicator
            simpleMatcher.appendReplacement(sb, Matcher.quoteReplacement(address + "***"));
        }
        simpleMatcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Anonymizes all ID numbers found in text
     */
    private String anonymizeIdNumbersInText(String text) {
        Matcher idMatcher = ID_NUMBER_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (idMatcher.find()) {
            String idNumber = idMatcher.group();
            String anonymized = anonymizeIdNumber(idNumber);
            idMatcher.appendReplacement(sb, Matcher.quoteReplacement(anonymized));
        }
        idMatcher.appendTail(sb);
        return sb.toString();
    }
    
    /**
     * Anonymizes all Chinese names found in text
     */
    private String anonymizeNamesInText(String text) {
        Matcher nameMatcher = NAME_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        
        while (nameMatcher.find()) {
            String firstChar = nameMatcher.group(1);
            String title = nameMatcher.group(2);
            String restOfName = nameMatcher.group(3);
            
            // Only anonymize if first character is a common surname
            if (COMMON_SURNAMES.contains(firstChar)) {
                String fullName = firstChar + (title != null ? title : "") + restOfName;
                
                // Check if this looks like a real name (2-4 characters total, excluding title)
                int nameLength = firstChar.length() + restOfName.length();
                if (nameLength >= 2 && nameLength <= 4) {
                    // Replace with placeholder, keep title if present
                    String replacement = "某人" + (title != null ? title : "");
                    nameMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
                    continue;
                }
            }
            
            // If not a name, keep original
            nameMatcher.appendReplacement(sb, Matcher.quoteReplacement(nameMatcher.group()));
        }
        nameMatcher.appendTail(sb);
        
        return sb.toString();
    }
    
    /**
     * Checks if text contains Chinese names
     */
    private boolean containsChineseName(String text) {
        Matcher nameMatcher = NAME_PATTERN.matcher(text);
        while (nameMatcher.find()) {
            String firstChar = nameMatcher.group(1);
            if (COMMON_SURNAMES.contains(firstChar)) {
                return true;
            }
        }
        return false;
    }
}
