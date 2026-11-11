package com.learning.silveragepass.dto.organization;

import com.learning.silveragepass.entity.OrganizationStatus;
import lombok.Data;

/**
 * 组织查询条件DTO
 */
@Data
public class OrganizationQueryDTO {

    private String keyword;

    private OrganizationStatus status;

    private Integer page = 0;

    private Integer size = 10;

    private String sortBy = "name";

    private String sortDirection = "ASC";
}
