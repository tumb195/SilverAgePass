package com.learning.silveragepass.service.impl;

import com.learning.silveragepass.config.ApiKeyProperties;
import com.learning.silveragepass.dto.weather.AirQualityResponse;
import com.learning.silveragepass.dto.weather.QweatherApiResponse;
import com.learning.silveragepass.dto.weather.WeatherForecastResponse;
import com.learning.silveragepass.dto.weather.WeatherNowResponse;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.service.WeatherProxyService;
import com.learning.silveragepass.util.LocationParser;
import com.learning.silveragepass.util.QweatherJwtUtil;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 天气代理服务实现
 * 代理和风天气 API 请求，实现 Redis 缓存
 * 支持两种认证方式：JWT（推荐）和 API Key
 */
@Service
public class WeatherProxyServiceImpl implements WeatherProxyService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherProxyServiceImpl.class);

    private static final String CACHE_KEY_PREFIX = "weather:";
    private static final String CACHE_KEY_NOW = "now:";
    private static final String CACHE_KEY_FORECAST = "forecast:";
    private static final String CACHE_KEY_AIR = "air:";
    
    // 缓存 TTL 配置（秒）- Requirements: 3.2, 3.3
    private static final long CACHE_TTL_NOW = 600;       // 实时天气：10分钟
    private static final long CACHE_TTL_FORECAST = 1800; // 天气预报：30分钟
    private static final long CACHE_TTL_AIR = 600;       // 空气质量：10分钟

    private final ApiKeyProperties apiKeyProperties;
    private final RestTemplate restTemplate;
    private final RedisTemplate<String, Object> redisTemplate;

    public WeatherProxyServiceImpl(
            ApiKeyProperties apiKeyProperties,
            RedisTemplate<String, Object> redisTemplate) {
        this.apiKeyProperties = apiKeyProperties;
        // 使用Apache HttpClient，自动处理gzip压缩
        CloseableHttpClient httpClient = HttpClients.custom()
                .build();
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        this.restTemplate = new RestTemplate(requestFactory);
        this.redisTemplate = redisTemplate;
    }

    @Override
    public WeatherNowResponse getWeatherNow(String location) {
        validateLocation(location);
        
        String cacheKey = buildCacheKey(CACHE_KEY_NOW, location);
        
        // Try to get from cache
        WeatherNowResponse cached = getCachedWeatherNow(cacheKey);
        if (cached != null) {
            logger.info("[缓存命中] 实时天气 location={}", location);
            return cached;
        }

        logger.info("[缓存未命中] 实时天气 location={}, 调用API", location);
        
        // Call Qweather API
        QweatherApiResponse apiResponse = callQweatherApi("/weather/now", location);
        
        // Convert to response DTO
        WeatherNowResponse response = convertToWeatherNowResponse(apiResponse, location);
        
        // Cache the response with specific TTL
        cacheResponseWithTtl(cacheKey, response, CACHE_TTL_NOW);
        
        return response;
    }

    @Override
    public WeatherForecastResponse getWeatherForecast(String location, int days) {
        validateLocation(location);
        validateDays(days);
        
        String cacheKey = buildCacheKey(CACHE_KEY_FORECAST + days + "d:", location);
        
        // Try to get from cache
        WeatherForecastResponse cached = getCachedWeatherForecast(cacheKey);
        if (cached != null) {
            logger.info("[缓存命中] 天气预报 location={}, days={}", location, days);
            return cached;
        }

        logger.info("[缓存未命中] 天气预报 location={}, days={}, 调用API", location, days);
        
        // Call Qweather API
        String path = "/weather/" + days + "d";
        QweatherApiResponse apiResponse = callQweatherApi(path, location);
        
        // Convert to response DTO
        WeatherForecastResponse response = convertToWeatherForecastResponse(apiResponse, location);
        
        // Cache the response with specific TTL
        cacheResponseWithTtl(cacheKey, response, CACHE_TTL_FORECAST);
        
        return response;
    }

    @Override
    public AirQualityResponse getAirQuality(String location) {
        validateLocation(location);
        
        String cacheKey = buildCacheKey(CACHE_KEY_AIR, location);
        
        // Try to get from cache
        AirQualityResponse cached = getCachedAirQuality(cacheKey);
        if (cached != null) {
            logger.info("[缓存命中] 空气质量 location={}", location);
            return cached;
        }

        logger.info("[缓存未命中] 空气质量 location={}, 调用API", location);
        
        // 检查位置格式
        String locationType = LocationParser.getLocationType(location);
        logger.info("[AirQuality] 位置类型: {}, 位置: {}", locationType, location);
        
        try {
            AirQualityResponse response;
            
            // 如果是经纬度格式，尝试使用新版空气质量 API
            if (LocationParser.isCoordinateFormat(location)) {
                logger.info("[AirQuality] 使用经纬度格式调用空气质量 API");
                response = callAirQualityApiWithCoordinates(location);
            } else {
                // 城市 ID 格式，使用旧版 API
                logger.info("[AirQuality] 使用城市 ID 格式调用空气质量 API");
                QweatherApiResponse apiResponse = callQweatherApi("/air/now", location);
                response = convertToAirQualityResponse(apiResponse, location);
            }
            
            // Cache the response with specific TTL
            cacheResponseWithTtl(cacheKey, response, CACHE_TTL_AIR);
            
            return response;
        } catch (BusinessException e) {
            // 如果是权限问题（403），返回一个默认的"暂无数据"响应
            logger.error("[AirQuality] API 调用失败: {}", e.getMessage());
            if (e.getMessage() != null && (e.getMessage().contains("403") || e.getMessage().contains("401") || e.getMessage().contains("402"))) {
                logger.warn("[AirQuality] 空气质量 API 权限不足，返回暂无数据响应");
                AirQualityResponse unavailable = new AirQualityResponse();
                unavailable.setLocation(location);
                unavailable.setCategory("暂无数据");
                unavailable.setAqi("--");
                return unavailable;
            }
            throw e;
        }
    }
    
    /**
     * 使用经纬度调用空气质量 API
     * 新版和风天气空气质量 API 使用路径参数格式: /airquality/v1/current/{lat}/{lon}
     * 
     * Requirements: 4.1, 4.2
     */
    private AirQualityResponse callAirQualityApiWithCoordinates(String location) {
        LocationParser.Coordinates coordinates = LocationParser.parseCoordinates(location);
        if (coordinates == null) {
            logger.error("[AirQuality] 无法解析经纬度: {}", location);
            throw new BusinessException("WEATHER_003", "无效的经纬度格式: " + location);
        }
        
        logger.info("[AirQuality] 解析坐标 - 经度: {}, 纬度: {}", 
                coordinates.getLongitude(), coordinates.getLatitude());
        
        // 使用新版空气质量 API（独立的 API Host 和密钥）
        try {
            logger.info("[AirQuality] 使用新版 /airquality/v1/current 端点");
            return callNewAirQualityApi(coordinates);
        } catch (BusinessException e) {
            logger.warn("[AirQuality] 新版空气质量 API 调用失败: {}", e.getMessage());
            
            // 如果新版 API 失败，返回暂无数据
            AirQualityResponse unavailable = new AirQualityResponse();
            unavailable.setLocation(location);
            unavailable.setCategory("暂无数据");
            unavailable.setAqi("--");
            return unavailable;
        }
    }
    
    /**
     * 调用新版空气质量 API
     * 使用独立的 API Host 和密钥
     * 路径格式: /airquality/v1/current/{latitude}/{longitude}
     */
    private AirQualityResponse callNewAirQualityApi(LocationParser.Coordinates coordinates) {
        try {
            // 格式化经纬度（保留2位小数）
            String lat = String.format("%.2f", coordinates.getLatitude());
            String lon = String.format("%.2f", coordinates.getLongitude());
            
            // 构建 URL
            String baseUrl = apiKeyProperties.getAirquality().getBaseUrl();
            String url = baseUrl + "/airquality/v1/current/" + lat + "/" + lon;
            
            logger.info("[AirQuality] 调用新版 API URL: {}", url);
            
            // 生成 JWT Token（使用空气质量 API 的独立密钥）
            String token = QweatherJwtUtil.generateToken(
                    apiKeyProperties.getAirquality().getPrivateKey(),
                    apiKeyProperties.getAirquality().getProjectId(),
                    apiKeyProperties.getAirquality().getCredentialId()
            );
            
            logger.debug("[AirQuality] Generated JWT token: {}...", token.substring(0, Math.min(50, token.length())));
            
            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            // 调用 API
            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url, HttpMethod.GET, entity, String.class);
            
            String responseBody = responseEntity.getBody();
            logger.debug("[AirQuality] API 响应: {}", responseBody);
            
            // 解析新版 API 响应
            return parseNewAirQualityResponse(responseBody, coordinates);
            
        } catch (RestClientException e) {
            logger.error("[AirQuality] 新版 API 调用失败: {}", e.getMessage(), e);
            throw new BusinessException("WEATHER_002", "空气质量服务暂时不可用: " + e.getMessage());
        }
    }
    
    /**
     * 解析新版空气质量 API 响应
     * 新版 API 返回格式与旧版不同，需要单独处理
     */
    private AirQualityResponse parseNewAirQualityResponse(String responseBody, LocationParser.Coordinates coordinates) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(responseBody);
            
            AirQualityResponse response = new AirQualityResponse();
            response.setLocation(coordinates.getLongitude() + "," + coordinates.getLatitude());
            
            // 解析 indexes 数组，优先使用中国标准 (cn-mep) 或美国标准 (us-epa)
            com.fasterxml.jackson.databind.JsonNode indexes = root.get("indexes");
            if (indexes != null && indexes.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode index : indexes) {
                    String code = index.has("code") ? index.get("code").asText() : "";
                    
                    // 优先使用中国标准，其次美国标准
                    if ("cn-mep".equals(code) || "us-epa".equals(code) || response.getAqi() == null) {
                        if (index.has("aqi")) {
                            response.setAqi(String.valueOf(index.get("aqi").asInt()));
                        }
                        if (index.has("category")) {
                            response.setCategory(index.get("category").asText());
                        }
                        if (index.has("primaryPollutant")) {
                            com.fasterxml.jackson.databind.JsonNode primary = index.get("primaryPollutant");
                            if (primary.has("name")) {
                                response.setPrimary(primary.get("name").asText());
                            }
                        }
                        
                        // 如果找到中国标准，直接使用
                        if ("cn-mep".equals(code)) {
                            break;
                        }
                    }
                }
            }
            
            // 解析 pollutants 数组
            com.fasterxml.jackson.databind.JsonNode pollutants = root.get("pollutants");
            if (pollutants != null && pollutants.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode pollutant : pollutants) {
                    String code = pollutant.has("code") ? pollutant.get("code").asText() : "";
                    String value = "";
                    
                    if (pollutant.has("concentration")) {
                        com.fasterxml.jackson.databind.JsonNode concentration = pollutant.get("concentration");
                        if (concentration.has("value")) {
                            value = String.valueOf(concentration.get("value").asDouble());
                        }
                    }
                    
                    switch (code) {
                        case "pm2p5":
                            response.setPm2p5(value);
                            break;
                        case "pm10":
                            response.setPm10(value);
                            break;
                        case "no2":
                            response.setNo2(value);
                            break;
                        case "so2":
                            response.setSo2(value);
                            break;
                        case "co":
                            response.setCo(value);
                            break;
                        case "o3":
                            response.setO3(value);
                            break;
                    }
                }
            }
            
            logger.info("[AirQuality] 解析成功 - AQI: {}, Category: {}", response.getAqi(), response.getCategory());
            return response;
            
        } catch (Exception e) {
            logger.error("[AirQuality] 解析响应失败: {}", e.getMessage(), e);
            throw new BusinessException("WEATHER_001", "空气质量数据解析失败: " + e.getMessage());
        }
    }

    @Override
    public void clearCache(String location) {
        if (location == null || location.trim().isEmpty()) {
            return;
        }
        
        String pattern = CACHE_KEY_PREFIX + "*:" + location;
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.info("Cleared {} cache entries for location: {}", keys.size(), location);
        }
    }

    @Override
    public void clearAllCache() {
        String pattern = CACHE_KEY_PREFIX + "*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.info("Cleared all {} weather cache entries", keys.size());
        }
    }

    // ==================== Private Helper Methods ====================

    private void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new BusinessException("WEATHER_003", "位置参数不能为空");
        }
    }

    private void validateDays(int days) {
        if (days != 3 && days != 7 && days != 10 && days != 15 && days != 30) {
            throw new BusinessException("WEATHER_003", "预报天数必须是 3, 7, 10, 15 或 30");
        }
    }

    private String buildCacheKey(String type, String location) {
        return CACHE_KEY_PREFIX + type + location;
    }

    /**
     * 判断是否使用 JWT 认证
     */
    private boolean useJwtAuth() {
        String authType = apiKeyProperties.getQweather().getAuthType();
        if ("jwt".equalsIgnoreCase(authType)) {
            // 检查 JWT 配置是否完整
            return apiKeyProperties.getQweather().getPrivateKey() != null
                    && apiKeyProperties.getQweather().getCredentialId() != null;
        }
        return false;
    }

    /**
     * 构建请求 URL（不含认证参数）
     */
    private String buildUrl(String path, String location) {
        return UriComponentsBuilder
                .fromHttpUrl(apiKeyProperties.getQweather().getBaseUrl() + path)
                .queryParam("location", location)
                .build()
                .toUriString();
    }

    /**
     * 调用和风天气 API
     * 根据配置自动选择 JWT 或 API Key 认证方式
     */
    private QweatherApiResponse callQweatherApi(String path, String location) {
        try {
            String url;
            ResponseEntity<QweatherApiResponse> responseEntity;

            if (useJwtAuth()) {
                // JWT 认证：通过 Authorization 请求头
                logger.debug("Using JWT authentication for Qweather API");
                
                String token = QweatherJwtUtil.generateToken(
                        apiKeyProperties.getQweather().getPrivateKey(),
                        apiKeyProperties.getQweather().getProjectId(),
                        apiKeyProperties.getQweather().getCredentialId()
                );
                
                logger.debug("Generated JWT token: {}...", token.substring(0, Math.min(50, token.length())));
                
                url = buildUrl(path, location);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + token);
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                logger.debug("Calling Qweather API URL: {} with JWT auth", url);
                responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, QweatherApiResponse.class);
                
            } else {
                // API Key 认证：通过 X-QW-Api-Key 请求头
                logger.debug("Using API Key authentication for Qweather API");
                
                url = buildUrl(path, location);
                
                HttpHeaders headers = new HttpHeaders();
                headers.set("X-QW-Api-Key", apiKeyProperties.getQweather().getKey());
                HttpEntity<String> entity = new HttpEntity<>(headers);
                
                logger.debug("Calling Qweather API URL: {} with API Key auth", url);
                responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, QweatherApiResponse.class);
            }
            
            QweatherApiResponse response = responseEntity.getBody();
            
            if (response == null) {
                logger.error("Qweather API returned null response");
                throw new BusinessException("WEATHER_001", "天气 API 返回空响应");
            }
            
            if (!response.isSuccess()) {
                logger.error("Qweather API returned error code: {}", response.getCode());
                throw new BusinessException("WEATHER_001", 
                        "天气 API 调用失败，错误码: " + response.getCode());
            }
            
            return response;
            
        } catch (RestClientException e) {
            logger.error("Failed to call Qweather API: {}", e.getMessage(), e);
            throw new BusinessException("WEATHER_002", "天气服务暂时不可用: " + e.getMessage());
        }
    }

    private WeatherNowResponse convertToWeatherNowResponse(QweatherApiResponse apiResponse, String location) {
        QweatherApiResponse.WeatherNow now = apiResponse.getNow();
        if (now == null) {
            throw new BusinessException("WEATHER_001", "天气 API 返回数据格式错误");
        }
        
        WeatherNowResponse response = new WeatherNowResponse();
        response.setLocation(location);
        response.setUpdateTime(apiResponse.getUpdateTime());
        response.setTemp(now.getTemp());
        response.setFeelsLike(now.getFeelsLike());
        response.setIcon(now.getIcon());
        response.setText(now.getText());
        response.setWindDir(now.getWindDir());
        response.setWindScale(now.getWindScale());
        response.setWindSpeed(now.getWindSpeed());
        response.setHumidity(now.getHumidity());
        response.setPressure(now.getPressure());
        response.setVis(now.getVis());
        response.setCloud(now.getCloud());
        response.setDew(now.getDew());
        
        return response;
    }

    private WeatherForecastResponse convertToWeatherForecastResponse(QweatherApiResponse apiResponse, String location) {
        if (apiResponse.getDaily() == null) {
            throw new BusinessException("WEATHER_001", "天气 API 返回数据格式错误");
        }
        
        WeatherForecastResponse response = new WeatherForecastResponse();
        response.setLocation(location);
        response.setUpdateTime(apiResponse.getUpdateTime());
        response.setDaily(apiResponse.getDaily());
        
        return response;
    }

    private AirQualityResponse convertToAirQualityResponse(QweatherApiResponse apiResponse, String location) {
        QweatherApiResponse.AirNow airNow = apiResponse.getAirNow();
        if (airNow == null) {
            throw new BusinessException("WEATHER_001", "空气质量 API 返回数据格式错误");
        }
        
        AirQualityResponse response = new AirQualityResponse();
        response.setLocation(location);
        response.setUpdateTime(apiResponse.getUpdateTime());
        response.setAqi(airNow.getAqi());
        response.setCategory(airNow.getCategory());
        response.setPrimary(airNow.getPrimary());
        response.setPm2p5(airNow.getPm2p5());
        response.setPm10(airNow.getPm10());
        response.setNo2(airNow.getNo2());
        response.setSo2(airNow.getSo2());
        response.setCo(airNow.getCo());
        response.setO3(airNow.getO3());
        
        return response;
    }

    private void cacheResponse(String cacheKey, Object response) {
        try {
            long ttlSeconds = apiKeyProperties.getQweather().getCacheTtlSeconds();
            redisTemplate.opsForValue().set(cacheKey, response, ttlSeconds, TimeUnit.SECONDS);
            logger.debug("Cached response with key: {}, TTL: {}s", cacheKey, ttlSeconds);
        } catch (Exception e) {
            // Log but don't fail if caching fails
            logger.warn("Failed to cache response: {}", e.getMessage());
        }
    }
    
    /**
     * 使用指定 TTL 缓存响应
     * Requirements: 3.2, 3.3
     */
    private void cacheResponseWithTtl(String cacheKey, Object response, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(cacheKey, response, ttlSeconds, TimeUnit.SECONDS);
            logger.info("[缓存写入] key={}, TTL={}s", cacheKey, ttlSeconds);
        } catch (Exception e) {
            // Log but don't fail if caching fails
            logger.warn("Failed to cache response: {}", e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private WeatherNowResponse getCachedWeatherNow(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof WeatherNowResponse) {
                return (WeatherNowResponse) cached;
            }
            // Handle case where Redis returns a LinkedHashMap
            if (cached != null) {
                return convertMapToWeatherNow(cached);
            }
        } catch (Exception e) {
            logger.warn("Failed to get cached weather now: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private WeatherForecastResponse getCachedWeatherForecast(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof WeatherForecastResponse) {
                return (WeatherForecastResponse) cached;
            }
            if (cached != null) {
                return convertMapToWeatherForecast(cached);
            }
        } catch (Exception e) {
            logger.warn("Failed to get cached weather forecast: {}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private AirQualityResponse getCachedAirQuality(String cacheKey) {
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof AirQualityResponse) {
                return (AirQualityResponse) cached;
            }
            if (cached != null) {
                return convertMapToAirQuality(cached);
            }
        } catch (Exception e) {
            logger.warn("Failed to get cached air quality: {}", e.getMessage());
        }
        return null;
    }

    // Conversion methods for handling Redis deserialization
    @SuppressWarnings("unchecked")
    private WeatherNowResponse convertMapToWeatherNow(Object obj) {
        if (!(obj instanceof java.util.Map)) {
            return null;
        }
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
        WeatherNowResponse response = new WeatherNowResponse();
        response.setLocation((String) map.get("location"));
        response.setUpdateTime((String) map.get("updateTime"));
        response.setTemp((String) map.get("temp"));
        response.setFeelsLike((String) map.get("feelsLike"));
        response.setIcon((String) map.get("icon"));
        response.setText((String) map.get("text"));
        response.setWindDir((String) map.get("windDir"));
        response.setWindScale((String) map.get("windScale"));
        response.setWindSpeed((String) map.get("windSpeed"));
        response.setHumidity((String) map.get("humidity"));
        response.setPressure((String) map.get("pressure"));
        response.setVis((String) map.get("vis"));
        response.setCloud((String) map.get("cloud"));
        response.setDew((String) map.get("dew"));
        return response;
    }

    @SuppressWarnings("unchecked")
    private WeatherForecastResponse convertMapToWeatherForecast(Object obj) {
        if (!(obj instanceof java.util.Map)) {
            return null;
        }
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
        WeatherForecastResponse response = new WeatherForecastResponse();
        response.setLocation((String) map.get("location"));
        response.setUpdateTime((String) map.get("updateTime"));
        // Daily list conversion would be complex, return null to trigger API call
        return null;
    }

    @SuppressWarnings("unchecked")
    private AirQualityResponse convertMapToAirQuality(Object obj) {
        if (!(obj instanceof java.util.Map)) {
            return null;
        }
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) obj;
        AirQualityResponse response = new AirQualityResponse();
        response.setLocation((String) map.get("location"));
        response.setUpdateTime((String) map.get("updateTime"));
        response.setAqi((String) map.get("aqi"));
        response.setCategory((String) map.get("category"));
        response.setPrimary((String) map.get("primary"));
        response.setPm2p5((String) map.get("pm2p5"));
        response.setPm10((String) map.get("pm10"));
        response.setNo2((String) map.get("no2"));
        response.setSo2((String) map.get("so2"));
        response.setCo((String) map.get("co"));
        response.setO3((String) map.get("o3"));
        return response;
    }
}
