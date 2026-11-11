package com.learning.silveragepass.dto.certification;

import com.learning.silveragepass.entity.CertificationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证查询条件DTO
 * 用于管理端查询认证列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CertificationQueryDTO {

    private CertificationStatus status;  // 认证状态过滤

    private String keyword;  // 关键词（组织名称或用户名）

    private Integer page = 0;  // 页码，从0开始

    private Integer size = 10;  // 每页大小
}
