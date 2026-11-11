package com.learning.silveragepass.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServiceProofDTO {
    @NotNull(message = "需求ID不能为空")
    private Long demandId;
    
    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;
    
    @NotBlank(message = "文字描述不能为空")
    private String description;
    
    @NotNull(message = "服务状态不能为空")
    private ProofType type; // START 或 END
    
    public enum ProofType {
        START, // 开始服务
        END    // 结束服务
    }
} 