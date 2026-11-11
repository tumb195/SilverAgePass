package com.learning.silveragepass.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 位置解析工具类
 * 
 * 用于解析和转换位置格式，支持：
 * - 经纬度格式：116.41,39.92
 * - 城市 ID 格式：101010100
 * 
 * Requirements: 4.1
 */
public class LocationParser {
    
    private static final Logger logger = LoggerFactory.getLogger(LocationParser.class);
    
    // 经纬度格式正则：经度,纬度
    private static final Pattern COORDINATE_PATTERN = Pattern.compile(
            "^(-?\\d+\\.?\\d*),(-?\\d+\\.?\\d*)$"
    );
    
    // 城市 ID 格式正则：纯数字
    private static final Pattern CITY_ID_PATTERN = Pattern.compile("^\\d{9,12}$");
    
    /**
     * 坐标信息
     */
    public static class Coordinates {
        private final double longitude;
        private final double latitude;
        
        public Coordinates(double longitude, double latitude) {
            this.longitude = longitude;
            this.latitude = latitude;
        }
        
        public double getLongitude() {
            return longitude;
        }
        
        public double getLatitude() {
            return latitude;
        }
        
        @Override
        public String toString() {
            return longitude + "," + latitude;
        }
    }
    
    /**
     * 判断位置是否为经纬度格式
     * 
     * @param location 位置字符串
     * @return true 如果是经纬度格式
     */
    public static boolean isCoordinateFormat(String location) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        return COORDINATE_PATTERN.matcher(location.trim()).matches();
    }
    
    /**
     * 判断位置是否为城市 ID 格式
     * 
     * @param location 位置字符串
     * @return true 如果是城市 ID 格式
     */
    public static boolean isCityIdFormat(String location) {
        if (location == null || location.trim().isEmpty()) {
            return false;
        }
        return CITY_ID_PATTERN.matcher(location.trim()).matches();
    }
    
    /**
     * 解析经纬度格式的位置
     * 
     * @param location 位置字符串（格式：经度,纬度）
     * @return 坐标对象，如果解析失败返回 null
     */
    public static Coordinates parseCoordinates(String location) {
        if (location == null || location.trim().isEmpty()) {
            return null;
        }
        
        Matcher matcher = COORDINATE_PATTERN.matcher(location.trim());
        if (!matcher.matches()) {
            return null;
        }
        
        try {
            double longitude = Double.parseDouble(matcher.group(1));
            double latitude = Double.parseDouble(matcher.group(2));
            
            // 验证经纬度范围
            if (longitude < -180 || longitude > 180) {
                logger.warn("Invalid longitude: {}", longitude);
                return null;
            }
            if (latitude < -90 || latitude > 90) {
                logger.warn("Invalid latitude: {}", latitude);
                return null;
            }
            
            return new Coordinates(longitude, latitude);
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse coordinates: {}", location);
            return null;
        }
    }
    
    /**
     * 格式化坐标用于空气质量 API 路径
     * 新版和风天气空气质量 API 使用路径参数：/airquality/v1/current/{lat}/{lon}
     * 
     * @param coordinates 坐标对象
     * @return 格式化的路径部分，如 "39.92/116.41"
     */
    public static String formatForAirQualityPath(Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }
        // 注意：空气质量 API 路径格式是 {lat}/{lon}，纬度在前
        return String.format("%.2f/%.2f", coordinates.getLatitude(), coordinates.getLongitude());
    }
    
    /**
     * 格式化位置用于空气质量 API 路径
     * 
     * @param location 位置字符串（经度,纬度 格式）
     * @return 格式化的路径部分，如 "39.92/116.41"
     */
    public static String formatLocationForAirQualityPath(String location) {
        Coordinates coordinates = parseCoordinates(location);
        return formatForAirQualityPath(coordinates);
    }
    
    /**
     * 验证位置格式是否有效
     * 
     * @param location 位置字符串
     * @return true 如果格式有效（经纬度或城市 ID）
     */
    public static boolean isValidLocation(String location) {
        return isCoordinateFormat(location) || isCityIdFormat(location);
    }
    
    /**
     * 获取位置类型描述
     * 
     * @param location 位置字符串
     * @return 位置类型描述
     */
    public static String getLocationType(String location) {
        if (isCoordinateFormat(location)) {
            return "coordinates";
        } else if (isCityIdFormat(location)) {
            return "cityId";
        } else {
            return "unknown";
        }
    }
}
