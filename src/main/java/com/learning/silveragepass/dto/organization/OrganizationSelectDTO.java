package com.learning.silveragepass.dto.organization;

import lombok.Data;

/**
 * 用户端组织选择项DTO
 */
@Data
public class OrganizationSelectDTO {

    private Long id;

    private String name;

    private String type;

    private String description;

    private String logoUrl;
}
