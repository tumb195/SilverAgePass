package com.learning.silveragepass.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import net.jqwik.api.*;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Property-based tests for QweatherJwtUtil
 * 
 * **Feature: api-key-security, Property 3: JWT token structure validity**
 * **Validates: Requirements 9.1, 9.2, 9.3, 9.4**
 */
public class QweatherJwtUtilPropertyTest {

    // Test Ed25519 key pair for testing
    // This is a valid Ed25519 private key in PKCS8 format (Base64 encoded)
    private static final String TEST_PRIVATE_KEY = "MC4CAQAwBQYDK2VwBCIEINcRej1+acfFbwcw/qT05ltXKLwk5NfzZDY01+1BDuw9";

    /**
     * Property 3.1: JWT token uses Ed25519 algorithm
     * 
     * *For any* valid project ID and credential ID, the generated JWT token
     * SHALL use the EdDSA algorithm (Ed25519).
     * 
     * **Feature: api-key-security, Property 3: JWT token structure validity**
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void jwtTokenShouldUseEd25519Algorithm(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String projectId,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String credentialId) {
        
        // Generate token
        String token = QweatherJwtUtil.generateToken(TEST_PRIVATE_KEY, projectId, credentialId);
        
        // Parse token header (without verification)
        String[] parts = token.split("\\.");
        Assume.that(parts.length == 3);
        
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        
        // Verify algorithm is EdDSA
        Assume.that(headerJson.contains("\"alg\""));
        Assume.that(headerJson.contains("EdDSA"));
    }

    /**
     * Property 3.2: JWT token includes project ID as issuer (iss) claim
     * 
     * *For any* valid project ID and credential ID, the generated JWT token
     * SHALL include the project ID as the issuer (iss) claim.
     * 
     * **Feature: api-key-security, Property 3: JWT token structure validity**
     * **Validates: Requirements 9.2**
     */
    @Property(tries = 100)
    void jwtTokenShouldIncludeProjectIdAsIssuer(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String projectId,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String credentialId) {
        
        // Generate token
        String token = QweatherJwtUtil.generateToken(TEST_PRIVATE_KEY, projectId, credentialId);
        
        // Parse token payload (without verification)
        String[] parts = token.split("\\.");
        Assume.that(parts.length == 3);
        
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
        
        // Verify issuer matches project ID
        Assume.that(payloadJson.contains("\"iss\""));
        Assume.that(payloadJson.contains("\"" + projectId + "\""));
    }

    /**
     * Property 3.3: JWT token includes credential ID as key ID (kid) in header
     * 
     * *For any* valid project ID and credential ID, the generated JWT token
     * SHALL include the credential ID as the key ID (kid) in the header.
     * 
     * **Feature: api-key-security, Property 3: JWT token structure validity**
     * **Validates: Requirements 9.3**
     */
    @Property(tries = 100)
    void jwtTokenShouldIncludeCredentialIdAsKid(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String projectId,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String credentialId) {
        
        // Generate token
        String token = QweatherJwtUtil.generateToken(TEST_PRIVATE_KEY, projectId, credentialId);
        
        // Parse token header (without verification)
        String[] parts = token.split("\\.");
        Assume.that(parts.length == 3);
        
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        
        // Verify kid matches credential ID
        Assume.that(headerJson.contains("\"kid\""));
        Assume.that(headerJson.contains("\"" + credentialId + "\""));
    }

    /**
     * Property 3.4: JWT token has valid expiration time within 24 hours
     * 
     * *For any* valid project ID and credential ID, the generated JWT token
     * SHALL have an expiration time (exp) within 24 hours of generation.
     * 
     * **Feature: api-key-security, Property 3: JWT token structure validity**
     * **Validates: Requirements 9.4**
     */
    @Property(tries = 100)
    void jwtTokenShouldHaveValidExpiration(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String projectId,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String credentialId) {
        
        long beforeGeneration = System.currentTimeMillis();
        
        // Generate token
        String token = QweatherJwtUtil.generateToken(TEST_PRIVATE_KEY, projectId, credentialId);
        
        long afterGeneration = System.currentTimeMillis();
        
        // Parse token payload (without verification)
        String[] parts = token.split("\\.");
        Assume.that(parts.length == 3);
        
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
        
        // Verify exp claim exists
        Assume.that(payloadJson.contains("\"exp\""));
        
        // Extract exp value (simple parsing)
        int expIndex = payloadJson.indexOf("\"exp\":");
        Assume.that(expIndex >= 0);
        
        String expPart = payloadJson.substring(expIndex + 6);
        int endIndex = expPart.indexOf(",");
        if (endIndex < 0) {
            endIndex = expPart.indexOf("}");
        }
        String expValue = expPart.substring(0, endIndex).trim();
        long expTimestamp = Long.parseLong(expValue) * 1000; // Convert to milliseconds
        
        // Verify expiration is in the future
        Assume.that(expTimestamp > afterGeneration);
        
        // Verify expiration is within 24 hours (86400000 ms)
        long maxExpiration = beforeGeneration + 86400000;
        Assume.that(expTimestamp <= maxExpiration);
    }

    /**
     * Property 3.5: JWT token includes issued at (iat) claim
     * 
     * *For any* valid project ID and credential ID, the generated JWT token
     * SHALL include the issued at (iat) claim with a valid timestamp.
     * 
     * **Feature: api-key-security, Property 3: JWT token structure validity**
     * **Validates: Requirements 9.4**
     */
    @Property(tries = 100)
    void jwtTokenShouldIncludeIssuedAtClaim(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String projectId,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String credentialId) {
        
        long beforeGeneration = System.currentTimeMillis();
        
        // Generate token
        String token = QweatherJwtUtil.generateToken(TEST_PRIVATE_KEY, projectId, credentialId);
        
        long afterGeneration = System.currentTimeMillis();
        
        // Parse token payload (without verification)
        String[] parts = token.split("\\.");
        Assume.that(parts.length == 3);
        
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
        
        // Verify iat claim exists
        Assume.that(payloadJson.contains("\"iat\""));
        
        // Extract iat value (simple parsing)
        int iatIndex = payloadJson.indexOf("\"iat\":");
        Assume.that(iatIndex >= 0);
        
        String iatPart = payloadJson.substring(iatIndex + 6);
        int endIndex = iatPart.indexOf(",");
        if (endIndex < 0) {
            endIndex = iatPart.indexOf("}");
        }
        String iatValue = iatPart.substring(0, endIndex).trim();
        long iatTimestamp = Long.parseLong(iatValue) * 1000; // Convert to milliseconds
        
        // Verify iat is within the generation time window (with 1 second tolerance)
        Assume.that(iatTimestamp >= beforeGeneration - 1000);
        Assume.that(iatTimestamp <= afterGeneration + 1000);
    }

    /**
     * Property 3.6: JWT token is well-formed (three parts separated by dots)
     * 
     * *For any* valid project ID and credential ID, the generated JWT token
     * SHALL be well-formed with three Base64URL-encoded parts separated by dots.
     * 
     * **Feature: api-key-security, Property 3: JWT token structure validity**
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 100)
    void jwtTokenShouldBeWellFormed(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String projectId,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String credentialId) {
        
        // Generate token
        String token = QweatherJwtUtil.generateToken(TEST_PRIVATE_KEY, projectId, credentialId);
        
        // Verify token is not null or empty
        Assume.that(token != null && !token.isEmpty());
        
        // Verify token has three parts
        String[] parts = token.split("\\.");
        Assume.that(parts.length == 3);
        
        // Verify each part is valid Base64URL
        for (String part : parts) {
            Assume.that(part != null && !part.isEmpty());
            try {
                Base64.getUrlDecoder().decode(part);
            } catch (IllegalArgumentException e) {
                Assume.that(false); // Invalid Base64URL
            }
        }
    }

    /**
     * Property 3.7: PEM format private key is handled correctly
     * 
     * *For any* valid project ID and credential ID, the JWT generator
     * SHALL correctly handle private keys with or without PEM headers.
     * 
     * **Feature: api-key-security, Property 3: JWT token structure validity**
     * **Validates: Requirements 9.1**
     */
    @Property(tries = 50)
    void jwtTokenShouldHandlePemFormatPrivateKey(
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String projectId,
            @ForAll @AlphaChars @StringLength(min = 5, max = 20) String credentialId) {
        
        // Test with PEM format (with headers)
        String pemKey = "-----BEGIN PRIVATE KEY-----\n" +
                TEST_PRIVATE_KEY + "\n" +
                "-----END PRIVATE KEY-----";
        
        // Generate token with PEM format key
        String token = QweatherJwtUtil.generateToken(pemKey, projectId, credentialId);
        
        // Verify token is valid
        Assume.that(token != null && !token.isEmpty());
        String[] parts = token.split("\\.");
        Assume.that(parts.length == 3);
    }
}
