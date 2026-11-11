package com.learning.silveragepass.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 空气质量响应 DTO
 * 映射和风天气 API 空气质量数据格式
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualityResponse {

    /**
     * 位置 ID
     */
    private String location;

    /**
     * 数据更新时间
     */
    private String updateTime;

    /**
     * 空气质量指数
     */
    private String aqi;

    /**
     * 空气质量等级
     */
    private String category;

    /**
     * 主要污染物
     */
    private String primary;

    /**
     * PM2.5
     */
    private String pm2p5;

    /**
     * PM10
     */
    private String pm10;

    /**
     * 二氧化氮
     */
    private String no2;

    /**
     * 二氧化硫
     */
    private String so2;

    /**
     * 一氧化碳
     */
    private String co;

    /**
     * 臭氧
     */
    private String o3;

    public AirQualityResponse() {
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public String getAqi() {
        return aqi;
    }

    public void setAqi(String aqi) {
        this.aqi = aqi;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPrimary() {
        return primary;
    }

    public void setPrimary(String primary) {
        this.primary = primary;
    }

    public String getPm2p5() {
        return pm2p5;
    }

    public void setPm2p5(String pm2p5) {
        this.pm2p5 = pm2p5;
    }

    public String getPm10() {
        return pm10;
    }

    public void setPm10(String pm10) {
        this.pm10 = pm10;
    }

    public String getNo2() {
        return no2;
    }

    public void setNo2(String no2) {
        this.no2 = no2;
    }

    public String getSo2() {
        return so2;
    }

    public void setSo2(String so2) {
        this.so2 = so2;
    }

    public String getCo() {
        return co;
    }

    public void setCo(String co) {
        this.co = co;
    }

    public String getO3() {
        return o3;
    }

    public void setO3(String o3) {
        this.o3 = o3;
    }
}
