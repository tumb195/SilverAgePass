package com.learning.silveragepass.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 和风天气空气质量 API 响应包装类
 * 用于解析 /air/now 接口返回的 JSON 数据
 * 
 * 注意：空气质量 API 返回的字段名是 "now"，与天气 API 相同，
 * 但内容是空气质量数据，因此需要单独的响应类
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class QweatherAirApiResponse {

    /**
     * API 状态码，200 表示成功
     */
    private String code;

    /**
     * 数据更新时间
     */
    private String updateTime;

    /**
     * 空气质量数据
     */
    private AirNow now;

    public QweatherAirApiResponse() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    public AirNow getNow() {
        return now;
    }

    public void setNow(AirNow now) {
        this.now = now;
    }

    /**
     * 判断 API 调用是否成功
     */
    public boolean isSuccess() {
        return "200".equals(code);
    }

    /**
     * 空气质量数据内部类
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AirNow {
        private String pubTime;
        private String aqi;
        private String level;
        private String category;
        private String primary;
        private String pm10;
        private String pm2p5;
        private String no2;
        private String so2;
        private String co;
        private String o3;

        public AirNow() {
        }

        public String getPubTime() {
            return pubTime;
        }

        public void setPubTime(String pubTime) {
            this.pubTime = pubTime;
        }

        public String getAqi() {
            return aqi;
        }

        public void setAqi(String aqi) {
            this.aqi = aqi;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
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

        public String getPm10() {
            return pm10;
        }

        public void setPm10(String pm10) {
            this.pm10 = pm10;
        }

        public String getPm2p5() {
            return pm2p5;
        }

        public void setPm2p5(String pm2p5) {
            this.pm2p5 = pm2p5;
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
}
