package com.learning.silveragepass.util;

import com.learning.silveragepass.dto.HeatMapGridDTO;
import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.DemandType;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网格聚合工具类
 * 将需求点按地理位置聚合到网格单元中，用于热力图展示
 * 
 * Requirements: 2.1, 2.2, 2.4
 */
public class GridAggregator {

    // 地球平均纬度每度对应的米数（近似值）
    private static final double METERS_PER_DEGREE_LAT = 111320.0;

    /**
     * 将需求点聚合到网格
     * 
     * @param demands 需求列表
     * @param centerLat 中心纬度
     * @param centerLng 中心经度
     * @param gridSizeMeters 网格大小（米）
     * @return 网格聚合结果列表
     */
    public static List<HeatMapGridDTO> aggregate(
            List<Demand> demands,
            Double centerLat,
            Double centerLng,
            Integer gridSizeMeters) {

        if (demands == null || demands.isEmpty()) {
            return Collections.emptyList();
        }

        if (centerLat == null || centerLng == null || gridSizeMeters == null || gridSizeMeters <= 0) {
            return Collections.emptyList();
        }

        // 将米转换为经纬度偏移量（近似）
        double latOffset = gridSizeMeters / METERS_PER_DEGREE_LAT;
        double lngOffset = gridSizeMeters / (METERS_PER_DEGREE_LAT * Math.cos(Math.toRadians(centerLat)));

        // 按网格分组 - 只处理有有效坐标的需求
        Map<String, List<Demand>> gridMap = demands.stream()
                .filter(d -> d != null && d.getLatitude() != null && d.getLongitude() != null)
                .collect(Collectors.groupingBy(d -> {
                    int gridX = (int) Math.floor((d.getLongitude() - centerLng) / lngOffset);
                    int gridY = (int) Math.floor((d.getLatitude() - centerLat) / latOffset);
                    return gridX + "," + gridY;
                }));

        // 转换为DTO
        return gridMap.entrySet().stream()
                .map(entry -> createGridDTO(entry.getKey(), entry.getValue(), centerLat, centerLng, latOffset, lngOffset))
                .collect(Collectors.toList());
    }


    /**
     * 创建单个网格DTO
     */
    private static HeatMapGridDTO createGridDTO(
            String gridKey,
            List<Demand> gridDemands,
            Double centerLat,
            Double centerLng,
            double latOffset,
            double lngOffset) {

        String[] coords = gridKey.split(",");
        int gridX = Integer.parseInt(coords[0]);
        int gridY = Integer.parseInt(coords[1]);

        HeatMapGridDTO dto = new HeatMapGridDTO();
        // 计算网格中心坐标（网格索引 + 0.5 得到中心点）
        dto.setCenterLatitude(centerLat + (gridY + 0.5) * latOffset);
        dto.setCenterLongitude(centerLng + (gridX + 0.5) * lngOffset);
        dto.setDemandCount(gridDemands.size());
        dto.setWeight(calculateGridWeight(gridDemands));
        dto.setTypeBreakdown(calculateTypeBreakdown(gridDemands));
        dto.setAvgDistance(null); // 平均距离需要在调用方计算（需要用户位置）

        return dto;
    }

    /**
     * 计算网格权重
     * 基于网格内需求的平均权重和数量加成
     * 
     * @param demands 网格内的需求列表
     * @return 权重值 (0-100)
     */
    public static int calculateGridWeight(List<Demand> demands) {
        if (demands == null || demands.isEmpty()) {
            return 0;
        }

        // 计算平均权重
        int avgWeight = (int) demands.stream()
                .mapToInt(WeightCalculator::calculateWeight)
                .average()
                .orElse(0);

        // 数量加成（每增加一个需求增加5分，最多加30分）
        int countBonus = Math.min(demands.size() * 5, 30);

        // 确保权重在0-100范围内
        return Math.min(avgWeight + countBonus, 100);
    }

    /**
     * 计算网格内需求的类型分布
     * 
     * @param demands 网格内的需求列表
     * @return 类型到数量的映射
     */
    public static Map<DemandType, Integer> calculateTypeBreakdown(List<Demand> demands) {
        if (demands == null || demands.isEmpty()) {
            return Collections.emptyMap();
        }

        return demands.stream()
                .filter(d -> d.getType() != null)
                .collect(Collectors.groupingBy(
                        Demand::getType,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    /**
     * 计算两点之间的距离（使用Haversine公式）
     * 
     * @param lat1 点1纬度
     * @param lng1 点1经度
     * @param lat2 点2纬度
     * @param lng2 点2经度
     * @return 距离（米）
     */
    public static double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371000; // 地球半径（米）

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad)
                * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 计算网格内需求到指定点的平均距离
     * 
     * @param demands 需求列表
     * @param targetLat 目标点纬度
     * @param targetLng 目标点经度
     * @return 平均距离（米）
     */
    public static Double calculateAvgDistance(List<Demand> demands, Double targetLat, Double targetLng) {
        if (demands == null || demands.isEmpty() || targetLat == null || targetLng == null) {
            return null;
        }

        List<Demand> validDemands = demands.stream()
                .filter(d -> d != null && d.getLatitude() != null && d.getLongitude() != null)
                .collect(Collectors.toList());

        if (validDemands.isEmpty()) {
            return null;
        }

        double totalDistance = validDemands.stream()
                .mapToDouble(d -> calculateDistance(targetLat, targetLng, d.getLatitude(), d.getLongitude()))
                .sum();

        return totalDistance / validDemands.size();
    }
}
