package com.learning.silveragepass.service;

import com.learning.silveragepass.dto.weather.AirQualityResponse;
import com.learning.silveragepass.dto.weather.WeatherForecastResponse;
import com.learning.silveragepass.dto.weather.WeatherNowResponse;

/**
 * 天气代理服务接口
 * 代理和风天气 API 请求，实现缓存和错误处理
 */
public interface WeatherProxyService {

    /**
     * 获取实时天气（带缓存）
     *
     * @param location 位置ID或经纬度（如 "101010100" 或 "116.41,39.92"）
     * @return 实时天气数据
     * @throws com.learning.silveragepass.exception.BusinessException 如果API调用失败
     */
    WeatherNowResponse getWeatherNow(String location);

    /**
     * 获取天气预报（带缓存）
     *
     * @param location 位置ID或经纬度
     * @param days     预报天数（3, 7, 10, 15, 30）
     * @return 天气预报数据
     * @throws com.learning.silveragepass.exception.BusinessException 如果API调用失败
     */
    WeatherForecastResponse getWeatherForecast(String location, int days);

    /**
     * 获取空气质量（带缓存）
     *
     * @param location 位置ID或经纬度
     * @return 空气质量数据
     * @throws com.learning.silveragepass.exception.BusinessException 如果API调用失败
     */
    AirQualityResponse getAirQuality(String location);

    /**
     * 清除指定位置的所有缓存
     *
     * @param location 位置ID或经纬度
     */
    void clearCache(String location);

    /**
     * 清除所有天气缓存
     */
    void clearAllCache();
}
