package com.learning.silveragepass.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 实时天气响应 DTO
 * 映射和风天气 API 实时天气数据格式
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherNowResponse {

    /**
     * 位置 ID
     */
    private String location;

    /**
     * 数据更新时间
     */
    private String updateTime;

    /**
     * 温度（摄氏度）
     */
    private String temp;

    /**
     * 体感温度（摄氏度）
     */
    private String feelsLike;

    /**
     * 天气状况图标代码
     */
    private String icon;

    /**
     * 天气状况文字描述
     */
    private String text;

    /**
     * 风向
     */
    private String windDir;

    /**
     * 风力等级
     */
    private String windScale;

    /**
     * 风速（公里/小时）
     */
    private String windSpeed;

    /**
     * 相对湿度（百分比）
     */
    private String humidity;

    /**
     * 大气压强（百帕）
     */
    private String pressure;

    /**
     * 能见度（公里）
     */
    private String vis;

    /**
     * 云量（百分比）
     */
    private String cloud;

    /**
     * 露点温度
     */
    private String dew;

    public WeatherNowResponse() {
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

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public String getFeelsLike() {
        return feelsLike;
    }

    public void setFeelsLike(String feelsLike) {
        this.feelsLike = feelsLike;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getWindDir() {
        return windDir;
    }

    public void setWindDir(String windDir) {
        this.windDir = windDir;
    }

    public String getWindScale() {
        return windScale;
    }

    public void setWindScale(String windScale) {
        this.windScale = windScale;
    }

    public String getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(String windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getPressure() {
        return pressure;
    }

    public void setPressure(String pressure) {
        this.pressure = pressure;
    }

    public String getVis() {
        return vis;
    }

    public void setVis(String vis) {
        this.vis = vis;
    }

    public String getCloud() {
        return cloud;
    }

    public void setCloud(String cloud) {
        this.cloud = cloud;
    }

    public String getDew() {
        return dew;
    }

    public void setDew(String dew) {
        this.dew = dew;
    }
}
