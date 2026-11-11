package com.learning.silveragepass.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 灵活的 LocalDate 反序列化器
 * 支持多种日期格式，包括：
 * - yyyy-MM-dd (标准 ISO 格式，如 2025-12-04)
 * - yyyy-M-d (灵活格式，如 2025-12-4)
 * - yyyy-M-dd (如 2025-1-04)
 * - yyyy-MM-d (如 2025-12-4)
 */
public class FlexibleLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    // 标准 ISO 格式
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    // 灵活格式（不要求前导零）
    private static final DateTimeFormatter FLEXIBLE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-M-d");

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String dateString = p.getText().trim();

        if (dateString == null || dateString.isEmpty()) {
            return null;
        }

        // 首先尝试标准 ISO 格式
        try {
            return LocalDate.parse(dateString, ISO_FORMATTER);
        } catch (DateTimeParseException e) {
            // 如果标准格式失败，尝试灵活格式
            try {
                return LocalDate.parse(dateString, FLEXIBLE_FORMATTER);
            } catch (DateTimeParseException e2) {
                // 如果都失败，抛出包含详细信息的异常
                throw new IOException(
                        String.format("无法解析日期字符串 '%s'。支持的格式: yyyy-MM-dd 或 yyyy-M-d", dateString),
                        e2);
            }
        }
    }
}









