package com.learning.silveragepass.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * 天气预报响应 DTO
 * 映射和风天气 API 天气预报数据格式
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherForecastResponse {

    /**
     * 位置 ID
     */
    private String location;

    /**
     * 数据更新时间
     */
    private String updateTime;

    /**
     * 每日预报列表
     */
    private List<DailyForecast> daily;

    public WeatherForecastResponse() {
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

    public List<DailyForecast> getDaily() {
        return daily;
    }

    public void setDaily(List<DailyForecast> daily) {
        this.daily = daily;
    }

    /**
     * 每日天气预报
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyForecast {

        /**
         * 预报日期
         */
        private String fxDate;

        /**
         * 日出时间
         */
        private String sunrise;

        /**
         * 日落时间
         */
        private String sunset;

        /**
         * 月升时间
         */
        private String moonrise;

        /**
         * 月落时间
         */
        private String moonset;

        /**
         * 月相名称
         */
        private String moonPhase;

        /**
         * 最高温度
         */
        private String tempMax;

        /**
         * 最低温度
         */
        private String tempMin;

        /**
         * 白天天气图标代码
         */
        private String iconDay;

        /**
         * 白天天气状况
         */
        private String textDay;

        /**
         * 夜间天气图标代码
         */
        private String iconNight;

        /**
         * 夜间天气状况
         */
        private String textNight;

        /**
         * 白天风向
         */
        private String windDirDay;

        /**
         * 白天风力等级
         */
        private String windScaleDay;

        /**
         * 白天风速
         */
        private String windSpeedDay;

        /**
         * 夜间风向
         */
        private String windDirNight;

        /**
         * 夜间风力等级
         */
        private String windScaleNight;

        /**
         * 夜间风速
         */
        private String windSpeedNight;

        /**
         * 相对湿度
         */
        private String humidity;

        /**
         * 降水量
         */
        private String precip;

        /**
         * 大气压强
         */
        private String pressure;

        /**
         * 能见度
         */
        private String vis;

        /**
         * 云量
         */
        private String cloud;

        /**
         * 紫外线强度指数
         */
        private String uvIndex;

        public DailyForecast() {
        }

        public String getFxDate() {
            return fxDate;
        }

        public void setFxDate(String fxDate) {
            this.fxDate = fxDate;
        }

        public String getSunrise() {
            return sunrise;
        }

        public void setSunrise(String sunrise) {
            this.sunrise = sunrise;
        }

        public String getSunset() {
            return sunset;
        }

        public void setSunset(String sunset) {
            this.sunset = sunset;
        }

        public String getMoonrise() {
            return moonrise;
        }

        public void setMoonrise(String moonrise) {
            this.moonrise = moonrise;
        }

        public String getMoonset() {
            return moonset;
        }

        public void setMoonset(String moonset) {
            this.moonset = moonset;
        }

        public String getMoonPhase() {
            return moonPhase;
        }

        public void setMoonPhase(String moonPhase) {
            this.moonPhase = moonPhase;
        }

        public String getTempMax() {
            return tempMax;
        }

        public void setTempMax(String tempMax) {
            this.tempMax = tempMax;
        }

        public String getTempMin() {
            return tempMin;
        }

        public void setTempMin(String tempMin) {
            this.tempMin = tempMin;
        }

        public String getIconDay() {
            return iconDay;
        }

        public void setIconDay(String iconDay) {
            this.iconDay = iconDay;
        }

        public String getTextDay() {
            return textDay;
        }

        public void setTextDay(String textDay) {
            this.textDay = textDay;
        }

        public String getIconNight() {
            return iconNight;
        }

        public void setIconNight(String iconNight) {
            this.iconNight = iconNight;
        }

        public String getTextNight() {
            return textNight;
        }

        public void setTextNight(String textNight) {
            this.textNight = textNight;
        }

        public String getWindDirDay() {
            return windDirDay;
        }

        public void setWindDirDay(String windDirDay) {
            this.windDirDay = windDirDay;
        }

        public String getWindScaleDay() {
            return windScaleDay;
        }

        public void setWindScaleDay(String windScaleDay) {
            this.windScaleDay = windScaleDay;
        }

        public String getWindSpeedDay() {
            return windSpeedDay;
        }

        public void setWindSpeedDay(String windSpeedDay) {
            this.windSpeedDay = windSpeedDay;
        }

        public String getWindDirNight() {
            return windDirNight;
        }

        public void setWindDirNight(String windDirNight) {
            this.windDirNight = windDirNight;
        }

        public String getWindScaleNight() {
            return windScaleNight;
        }

        public void setWindScaleNight(String windScaleNight) {
            this.windScaleNight = windScaleNight;
        }

        public String getWindSpeedNight() {
            return windSpeedNight;
        }

        public void setWindSpeedNight(String windSpeedNight) {
            this.windSpeedNight = windSpeedNight;
        }

        public String getHumidity() {
            return humidity;
        }

        public void setHumidity(String humidity) {
            this.humidity = humidity;
        }

        public String getPrecip() {
            return precip;
        }

        public void setPrecip(String precip) {
            this.precip = precip;
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

        public String getUvIndex() {
            return uvIndex;
        }

        public void setUvIndex(String uvIndex) {
            this.uvIndex = uvIndex;
        }
    }
}
