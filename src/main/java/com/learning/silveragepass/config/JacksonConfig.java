package com.learning.silveragepass.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.LocalDate;

/**
 * Jackson 配置类
 * 注册自定义的 LocalDate 反序列化器，支持灵活的日期格式
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置 ObjectMapper，注册自定义的 LocalDate 反序列化器
     * 使用 @Primary 确保这是主要的 ObjectMapper Bean
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();

        // 创建自定义模块
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDate.class, new FlexibleLocalDateDeserializer());

        // 注册模块
        objectMapper.registerModule(module);

        return objectMapper;
    }
}









