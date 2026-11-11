package com.learning.silveragepass.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户信息更新DTO
 * 用于更新用户的非敏感信息，不包含用户名、密码、手机号等关键字段
 */
@Data
public class UserUpdateDTO {
    
    @Size(max = 50, message = "真实姓名长度不能超过50个字符")
    private String realName;
    
    @Size(max = 255, message = "头像URL长度不能超过255个字符")
    private String avatar;
    
    @Min(value = 0, message = "年龄不能小于0")
    @Max(value = 150, message = "年龄不能大于150")
    private Integer age;
    
    @Size(max = 255, message = "地址长度不能超过255个字符")
    private String address;
    
    @Email(message = "邮箱格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过100个字符")
    private String email;
}

