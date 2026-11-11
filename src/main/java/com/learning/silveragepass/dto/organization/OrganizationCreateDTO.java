package com.learning.silveragepass.dto.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建组织请求DTO
 */
@Data
public class OrganizationCreateDTO {

    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称不能超过100个字符")
    private String name;

    @Size(max = 50, message = "组织类型不能超过50个字符")
    private String type;

    @Size(max = 500, message = "组织描述不能超过500个字符")
    private String description;

    @Size(max = 200, message = "组织地址不能超过200个字符")
    private String address;

    @Size(max = 50, message = "联系电话不能超过50个字符")
    private String contactPhone;

    @Size(max = 100, message = "联系人不能超过100个字符")
    private String contactPerson;

    @Size(max = 500, message = "Logo URL不能超过500个字符")
    private String logoUrl;
}
