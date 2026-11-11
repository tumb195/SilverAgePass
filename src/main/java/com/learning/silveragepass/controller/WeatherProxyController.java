package com.learning.silveragepass.controller;

import com.learning.silveragepass.dto.ApiResponse;
import com.learning.silveragepass.dto.weather.AirQualityResponse;
import com.learning.silveragepass.dto.weather.WeatherForecastResponse;
import com.learning.silveragepass.dto.weather.WeatherNowResponse;
import com.learning.silveragepass.exception.BusinessException;
import com.learning.silveragepass.security.CurrentUser;
import com.learning.silveragepass.security.UserPrincipal;
import com.learning.silveragepass.service.WeatherProxyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 天气代理控制器
 * 代理和风天气 API 请求，保护 API Key 不暴露给客户端
 */
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/weather")
@Tag(name = "天气服务", description = "天气数据代理接口")
public class WeatherProxyController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherProxyController.class);

    private final WeatherProxyService weatherProxyService;

    public WeatherProxyController(WeatherProxyService weatherProxyService) {
        this.weatherProxyService = weatherProxyService;
    }

    /**
     * 获取实时天气
     *
     * @param location 位置ID或经纬度（如 "101010100" 或 "116.41,39.92"）
     * @param user     当前登录用户
     * @return 实时天气数据
     */
    @GetMapping("/now")
    @Operation(summary = "获取实时天气", description = "根据位置获取实时天气数据")
    public ResponseEntity<ApiResponse<WeatherNowResponse>> getWeatherNow(
            @Parameter(description = "位置ID或经纬度") @RequestParam String location,
            @Parameter(hidden = true) @CurrentUser UserPrincipal user) {

        if (user == null) {
            logger.warn("Unauthenticated request for weather now");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "未认证的请求，请先登录"));
        }

        logger.debug("User {} requesting weather now for location: {}", user.getId(), location);

        try {
            WeatherNowResponse weather = weatherProxyService.getWeatherNow(location);
            return ResponseEntity.ok(ApiResponse.success("获取成功", weather));
        } catch (BusinessException e) {
            logger.error("Failed to get weather now: {}", e.getMessage());
            return handleWeatherError(e);
        }
    }

    /**
     * 获取天气预报
     *
     * @param location 位置ID或经纬度
     * @param days     预报天数（3, 7, 10, 15, 30），默认3天
     * @param user     当前登录用户
     * @return 天气预报数据
     */
    @GetMapping("/forecast")
    @Operation(summary = "获取天气预报", description = "根据位置获取未来几天的天气预报")
    public ResponseEntity<ApiResponse<WeatherForecastResponse>> getWeatherForecast(
            @Parameter(description = "位置ID或经纬度") @RequestParam String location,
            @Parameter(description = "预报天数(3,7,10,15,30)") @RequestParam(defaultValue = "3") Integer days,
            @Parameter(hidden = true) @CurrentUser UserPrincipal user) {

        if (user == null) {
            logger.warn("Unauthenticated request for weather forecast");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "未认证的请求，请先登录"));
        }

        logger.debug("User {} requesting weather forecast for location: {}, days: {}", 
                user.getId(), location, days);

        try {
            WeatherForecastResponse forecast = weatherProxyService.getWeatherForecast(location, days);
            return ResponseEntity.ok(ApiResponse.success("获取成功", forecast));
        } catch (BusinessException e) {
            logger.error("Failed to get weather forecast: {}", e.getMessage());
            return handleWeatherError(e);
        }
    }

    /**
     * 获取空气质量
     *
     * @param location 位置ID或经纬度
     * @param user     当前登录用户
     * @return 空气质量数据
     */
    @GetMapping("/air")
    @Operation(summary = "获取空气质量", description = "根据位置获取空气质量数据")
    public ResponseEntity<ApiResponse<AirQualityResponse>> getAirQuality(
            @Parameter(description = "位置ID或经纬度") @RequestParam String location,
            @Parameter(hidden = true) @CurrentUser UserPrincipal user) {

        if (user == null) {
            logger.warn("Unauthenticated request for air quality");
            return ResponseEntity.status(401)
                    .body(ApiResponse.error(401, "未认证的请求，请先登录"));
        }

        logger.debug("User {} requesting air quality for location: {}", user.getId(), location);

        try {
            AirQualityResponse airQuality = weatherProxyService.getAirQuality(location);
            return ResponseEntity.ok(ApiResponse.success("获取成功", airQuality));
        } catch (BusinessException e) {
            logger.error("Failed to get air quality: {}", e.getMessage());
            return handleWeatherError(e);
        }
    }

    /**
     * 处理天气服务错误
     */
    @SuppressWarnings("unchecked")
    private <T> ResponseEntity<ApiResponse<T>> handleWeatherError(BusinessException e) {
        String code = e.getCode();
        int httpStatus;
        
        switch (code) {
            case "WEATHER_001":
                // API 调用失败
                httpStatus = 502;
                break;
            case "WEATHER_002":
                // 服务暂时不可用
                httpStatus = 503;
                break;
            case "WEATHER_003":
                // 无效参数
                httpStatus = 400;
                break;
            default:
                httpStatus = 500;
        }
        
        return ResponseEntity.status(httpStatus)
                .body((ApiResponse<T>) ApiResponse.error(httpStatus, e.getMessage()));
    }
}
