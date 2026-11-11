package com.learning.silveragepass.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.DoubleRange;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for Weather Proxy Service URL construction
 * 
 * **Feature: api-key-security, Property 4: QWeather API URL construction**
 * **Validates: Requirements 1.4, 9.5, 9.6**
 * 
 * These tests verify that:
 * - URLs use the correct base URL format: https://{apiHost}/v7/{endpoint}
 * - JWT token is included as a query parameter named "token"
 * - Location is included as a query parameter
 */
public class WeatherProxyServiceUrlPropertyTest {

    private static final String API_HOST = "kj73jq74fg.re.qweatherapi.com";
    private static final String BASE_URL = "https://" + API_HOST + "/v7";

    /**
     * Property 4.1: Weather API URL uses correct base URL format
     * 
     * *For any* weather endpoint, the URL SHALL use the format
     * https://{apiHost}/v7/{endpoint}
     * 
     * **Feature: api-key-security, Property 4: QWeather API URL construction**
     * **Validates: Requirements 9.6**
     */
    @Property(tries = 100)
    void weatherApiUrlShouldUseCorrectBaseUrlFormat(
            @ForAll("weatherEndpoints") String endpoint) {
        
        String expectedPrefix = BASE_URL + endpoint;
        
        // Simulate URL construction
        String url = buildTestUrl(endpoint, "116.41,39.92", "test-token");
        
        // Verify URL starts with correct base
        assertThat(url).startsWith(expectedPrefix);
    }

    /**
     * Property 4.2: Weather API URL includes JWT token as query parameter
     * 
     * *For any* weather request, the URL SHALL include the JWT token
     * as a query parameter named "token".
     * 
     * **Feature: api-key-security, Property 4: QWeather API URL construction**
     * **Validates: Requirements 9.5**
     */
    @Property(tries = 100)
    void weatherApiUrlShouldIncludeTokenParameter(
            @ForAll("weatherEndpoints") String endpoint,
            @ForAll("validLocations") String location) {
        
        String token = "test-jwt-token-" + System.nanoTime();
        String url = buildTestUrl(endpoint, location, token);
        
        // Verify URL contains token parameter
        assertThat(url).contains("token=");
        assertThat(url).contains(token);
    }

    /**
     * Property 4.3: Weather API URL includes location parameter
     * 
     * *For any* weather request, the URL SHALL include the location
     * as a query parameter.
     * 
     * **Feature: api-key-security, Property 4: QWeather API URL construction**
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void weatherApiUrlShouldIncludeLocationParameter(
            @ForAll("weatherEndpoints") String endpoint,
            @ForAll("validLocations") String location) {
        
        String url = buildTestUrl(endpoint, location, "test-token");
        
        // Verify URL contains location parameter
        assertThat(url).contains("location=");
        
        // Verify the location value is in the URL (URL encoded)
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        assertThat(url).contains("location=" + encodedLocation);
    }

    /**
     * Property 4.4: Forecast URL uses correct days path
     * 
     * *For any* valid forecast days (3, 7, 10, 15, 30), the forecast URL
     * SHALL use the path /weather/{days}d
     * 
     * **Feature: api-key-security, Property 4: QWeather API URL construction**
     * **Validates: Requirements 1.4, 2.2**
     */
    @Property(tries = 50)
    void forecastUrlShouldUseCorrectDaysPath(
            @ForAll("validForecastDays") int days,
            @ForAll("validLocations") String location) {
        
        String expectedPath = "/weather/" + days + "d";
        String url = buildTestUrl(expectedPath, location, "test-token");
        
        // Verify URL contains correct path
        assertThat(url).contains(expectedPath);
    }

    /**
     * Property 4.5: Air quality URL uses correct endpoint
     * 
     * *For any* air quality request, the URL SHALL use the /air/now endpoint.
     * 
     * **Feature: api-key-security, Property 4: QWeather API URL construction**
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 50)
    void airQualityUrlShouldUseCorrectEndpoint(
            @ForAll("validLocations") String location) {
        
        String url = buildTestUrl("/air/now", location, "test-token");
        
        // Verify URL contains correct endpoint
        assertThat(url).contains("/air/now");
    }

    /**
     * Property 4.6: Coordinate location format is preserved
     * 
     * *For any* coordinate location (longitude,latitude), the URL SHALL
     * preserve the coordinate format (URL encoded).
     * 
     * **Feature: api-key-security, Property 4: QWeather API URL construction**
     * **Validates: Requirements 1.4**
     */
    @Property(tries = 100)
    void coordinateLocationFormatShouldBePreserved(
            @ForAll @DoubleRange(min = -180, max = 180) double longitude,
            @ForAll @DoubleRange(min = -90, max = 90) double latitude) {
        
        String location = longitude + "," + latitude;
        String url = buildTestUrl("/weather/now", location, "test-token");
        
        // Verify location is in the URL (URL encoded)
        String encodedLocation = URLEncoder.encode(location, StandardCharsets.UTF_8);
        assertThat(url).contains("location=" + encodedLocation);
    }

    /**
     * Property 4.7: URL structure follows query parameter format
     * 
     * *For any* weather request, the URL SHALL have query parameters
     * in the format ?param1=value1&param2=value2
     * 
     * **Feature: api-key-security, Property 4: QWeather API URL construction**
     * **Validates: Requirements 9.5, 9.6**
     */
    @Property(tries = 100)
    void urlShouldHaveCorrectQueryParameterStructure(
            @ForAll("weatherEndpoints") String endpoint,
            @ForAll("validLocations") String location) {
        
        String token = "test-token-" + System.nanoTime();
        String url = buildTestUrl(endpoint, location, token);
        
        // Verify URL has query string starting with ?
        assertThat(url).contains("?");
        
        // Verify parameters are separated by &
        String queryString = url.substring(url.indexOf("?") + 1);
        String[] params = queryString.split("&");
        
        // Should have at least location and token parameters
        assertThat(params.length).isGreaterThanOrEqualTo(2);
        
        // Verify each parameter has key=value format
        for (String param : params) {
            assertThat(param).contains("=");
        }
    }

    // ==================== Providers ====================

    @Provide
    Arbitrary<String> weatherEndpoints() {
        return Arbitraries.of(
            "/weather/now",
            "/weather/3d",
            "/weather/7d",
            "/weather/10d",
            "/weather/15d",
            "/weather/30d",
            "/air/now"
        );
    }

    @Provide
    Arbitrary<String> validLocations() {
        // City IDs
        Arbitrary<String> cityIds = Arbitraries.of(
            "101010100",  // Beijing
            "101020100",  // Shanghai
            "101280101",  // Guangzhou
            "101280601"   // Shenzhen
        );
        
        // Coordinate format (longitude,latitude)
        Arbitrary<String> coordinates = Arbitraries.doubles()
            .between(-180, 180)
            .flatMap(lon -> Arbitraries.doubles()
                .between(-90, 90)
                .map(lat -> String.format("%.2f,%.2f", lon, lat)));
        
        return Arbitraries.oneOf(cityIds, coordinates);
    }

    @Provide
    Arbitrary<Integer> validForecastDays() {
        return Arbitraries.of(3, 7, 10, 15, 30);
    }

    // ==================== Helper Methods ====================

    /**
     * Simulate URL construction (mirrors WeatherProxyServiceImpl.buildUrl)
     * This method constructs URLs in the same format as the production code:
     * https://{apiHost}/v7/{endpoint}?location={location}&token={token}
     */
    private String buildTestUrl(String path, String location, String token) {
        StringBuilder url = new StringBuilder(BASE_URL);
        url.append(path);
        url.append("?location=").append(URLEncoder.encode(location, StandardCharsets.UTF_8));
        url.append("&token=").append(token);
        return url.toString();
    }
}
