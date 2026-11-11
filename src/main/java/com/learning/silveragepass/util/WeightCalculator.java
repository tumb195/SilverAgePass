package com.learning.silveragepass.util;

import com.learning.silveragepass.entity.Demand;
import com.learning.silveragepass.entity.DemandType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 热力图权重计算工具类
 * 根据需求的服务日期紧急程度和需求类型优先级计算权重值
 * 权重值范围为 0-100
 */
public class WeightCalculator {

    private static final int BASE_WEIGHT = 50;
    private static final int MAX_WEIGHT = 100;
    private static final int MIN_WEIGHT = 0;

    /**
     * 计算单个需求的权重
     * 考虑因素：
     * 1. 服务日期临近程度 (0-30分)
     * 2. 需求类型优先级 (0-20分)
     *
     * @param demand 需求实体
     * @return 权重值 (0-100)
     */
    public static int calculateWeight(Demand demand) {
        if (demand == null) {
            return MIN_WEIGHT;
        }

        int weight = BASE_WEIGHT;

        // 时间紧急度加权
        weight += calculateUrgencyBonus(demand.getServiceDate());

        // 类型优先级加权
        weight += getTypePriority(demand.getType());

        // 确保权重在有效范围内
        return normalizeWeight(weight);
    }

    /**
     * 根据服务日期计算紧急度加成
     * - 24小时内（1天内）: +30分
     * - 3天内: +20分
     * - 7天内: +10分
     * - 超过7天或无日期: +0分
     *
     * @param serviceDate 服务日期
     * @return 紧急度加成分数
     */
    private static int calculateUrgencyBonus(LocalDate serviceDate) {
        if (serviceDate == null) {
            return 0;
        }

        long daysUntilService = ChronoUnit.DAYS.between(LocalDate.now(), serviceDate);
        
        // 如果服务日期已过，不加分
        if (daysUntilService < 0) {
            return 0;
        }

        if (daysUntilService <= 1) {
            return 30;  // 24小时内（今天或明天）
        } else if (daysUntilService <= 3) {
            return 20;  // 3天内
        } else if (daysUntilService <= 7) {
            return 10;  // 一周内
        }

        return 0;
    }

    /**
     * 获取需求类型的优先级加成
     * 健康服务类和出行辅助类优先级最高
     *
     * @param type 需求类型
     * @return 类型优先级加成分数 (0-20)
     */
    public static int getTypePriority(DemandType type) {
        if (type == null) {
            return 5;
        }

        return switch (type) {
            case HEALTH_SERVICE -> 20;      // 健康服务类 - 最高优先级
            case TRAVEL_ASSISTANCE -> 20;   // 出行辅助类 - 最高优先级
            case LIFE_CARE -> 15;           // 生活照料类
            case GOVERNMENT_ASSISTANCE -> 10; // 政务协助类
            case MENTAL_COMFORT -> 10;      // 精神慰藉类
            case OTHER -> 5;                // 其他
        };
    }

    /**
     * 将权重值归一化到 0-100 范围
     *
     * @param weight 原始权重值
     * @return 归一化后的权重值
     */
    private static int normalizeWeight(int weight) {
        return Math.max(MIN_WEIGHT, Math.min(weight, MAX_WEIGHT));
    }
}
