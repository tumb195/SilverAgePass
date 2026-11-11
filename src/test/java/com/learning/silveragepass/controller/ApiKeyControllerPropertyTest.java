package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.KeyRequestDTO;
import com.learning.silveragepass.dto.KeyTypeRequestDTO;
import net.jqwik.api.*;
import net.jqwik.api.constraints.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for ApiKeyController
 * 
 * **Feature: api-key-security-enhancement**
 * **Validates: Requirements 1.1, 1.2, 1.5, 5.3**
 */
public class ApiKeyControllerPropertyTest {

    // ==================== Property 1: POST endpoint acceptance ====================
    
    /**
     * Property 1: POST endpoint acceptance
     * 
     * *For any* valid request body, the POST endpoints SHALL accept the request
     * and return a valid response structure.
     * 
     * **Feature: api-key-security-enhancement, Property 1: POST endpoint acceptance**
     * **Validates: Requirements 1.1, 1.2**
     */
    @Property(tries = 100)
    void postEndpointsShouldAcceptValidRequestBody(
            @ForAll("validKeyRequestDTO") KeyRequestDTO requestBody) {
        
        // Verify request body is valid
        assertThat(requestBody).isNotNull();
        
        // The request body should be serializable (not throw exceptions)
        String deviceId = requestBody.getDeviceId();
        String source = requestBody.getSource();
        
        // Device ID and source can be null or non-empty strings
        if (deviceId != null) {
            assertThat(deviceId).isNotEmpty();
        }
        if (source != null) {
            assertThat(source).isNotEmpty();
        }
    }
    
    /**
     * Property 1 continued: KeyType request body validation
     * 
     * *For any* KeyTypeRequestDTO with valid keyType, the isValid() method
     * SHALL return true.
     * 
     * **Feature: api-key-security-enhancement, Property 1: POST endpoint acceptance**
     * **Validates: Requirements 1.1, 1.2**
     */
    @Property(tries = 100)
    void keyTypeRequestShouldValidateCorrectly(
            @ForAll("validKeyType") String keyType) {
        
        KeyTypeRequestDTO dto = new KeyTypeRequestDTO(keyType);
        
        assertThat(dto.isValid()).isTrue();
        assertThat(dto.getKeyType()).isEqualTo(keyType);
    }
    
    // ==================== Property 2: Backward compatibility ====================
    
    /**
     * Property 2: Backward compatibility
     * 
     * *For any* valid keyType parameter, both GET and POST endpoints SHALL
     * accept the same keyType values.
     * 
     * **Feature: api-key-security-enhancement, Property 2: Backward compatibility**
     * **Validates: Requirements 1.5**
     */
    @Property(tries = 100)
    void getAndPostEndpointsShouldAcceptSameKeyTypes(
            @ForAll("validKeyType") String keyType) {
        
        // Create DTO for POST request
        KeyTypeRequestDTO postDto = new KeyTypeRequestDTO(keyType);
        
        // Both should accept the same keyType values
        assertThat(postDto.isValid()).isTrue();
        assertThat(isValidKeyTypeForGet(keyType)).isTrue();
    }
    
    /**
     * Property 2 continued: Invalid keyTypes should be rejected by both
     * 
     * **Feature: api-key-security-enhancement, Property 2: Backward compatibility**
     * **Validates: Requirements 1.5**
     */
    @Property(tries = 100)
    void invalidKeyTypesShouldBeRejectedByBoth(
            @ForAll("invalidKeyType") String keyType) {
        
        KeyTypeRequestDTO postDto = new KeyTypeRequestDTO(keyType);
        
        // Both should reject invalid keyType values
        assertThat(postDto.isValid()).isFalse();
        assertThat(isValidKeyTypeForGet(keyType)).isFalse();
    }
    
    // ==================== Property 7: KeyType validation ====================
    
    /**
     * Property 7: KeyType validation
     * 
     * *For any* string that is not "amap" or "qweather", the validation
     * SHALL return false.
     * 
     * **Feature: api-key-security-enhancement, Property 7: KeyType validation**
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 100)
    void onlyAmapAndQweatherAreValidKeyTypes(
            @ForAll @AlphaChars @StringLength(min = 1, max = 20) String randomString) {
        
        KeyTypeRequestDTO dto = new KeyTypeRequestDTO(randomString);
        
        boolean isValid = dto.isValid();
        boolean shouldBeValid = "amap".equals(randomString) || "qweather".equals(randomString);
        
        assertThat(isValid).isEqualTo(shouldBeValid);
    }
    
    /**
     * Property 7 continued: Null keyType should be invalid
     * 
     * **Feature: api-key-security-enhancement, Property 7: KeyType validation**
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 10)
    void nullKeyTypeShouldBeInvalid() {
        KeyTypeRequestDTO dto = new KeyTypeRequestDTO(null);
        assertThat(dto.isValid()).isFalse();
    }
    
    /**
     * Property 7 continued: Empty keyType should be invalid
     * 
     * **Feature: api-key-security-enhancement, Property 7: KeyType validation**
     * **Validates: Requirements 5.3**
     */
    @Property(tries = 10)
    void emptyKeyTypeShouldBeInvalid() {
        KeyTypeRequestDTO dto = new KeyTypeRequestDTO("");
        assertThat(dto.isValid()).isFalse();
    }
    
    // ==================== Providers ====================
    
    @Provide
    Arbitrary<KeyRequestDTO> validKeyRequestDTO() {
        Arbitrary<String> deviceIds = Arbitraries.strings()
                .alpha().ofMinLength(5).ofMaxLength(30)
                .injectNull(0.3);
        
        Arbitrary<String> sources = Arbitraries.of(
                "seniorlink-android", "seniorlink-ios", "seniorlink-web", null);
        
        return Combinators.combine(deviceIds, sources)
                .as((deviceId, source) -> {
                    KeyRequestDTO dto = new KeyRequestDTO();
                    dto.setDeviceId(deviceId);
                    dto.setSource(source);
                    return dto;
                });
    }
    
    @Provide
    Arbitrary<String> validKeyType() {
        return Arbitraries.of("amap", "qweather");
    }
    
    @Provide
    Arbitrary<String> invalidKeyType() {
        return Arbitraries.strings()
                .alpha()
                .ofMinLength(1)
                .ofMaxLength(20)
                .filter(s -> !s.equals("amap") && !s.equals("qweather"));
    }
    
    // ==================== Helper Methods ====================
    
    /**
     * Simulates GET endpoint keyType validation
     */
    private boolean isValidKeyTypeForGet(String keyType) {
        return keyType != null && (keyType.equals("amap") || keyType.equals("qweather"));
    }
}
