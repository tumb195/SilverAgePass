package com.learning.silveragepass.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 用户筛选选项DTO
 * 用于返回前端可用的筛选选项（角色、状态）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFilterOptionsDTO {
    
    /**
     * 角色选项列表
     */
    private List<OptionItem> roles;
    
    /**
     * 状态选项列表
     */
    private List<OptionItem> statuses;
    
    /**
     * 选项项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        /**
         * 选项值（用于API请求）
         */
        private String value;
        
        /**
         * 选项显示文本（用于前端显示）
         */
        private String label;
        
        /**
         * 选项描述
         */
        private String description;
    }
}
