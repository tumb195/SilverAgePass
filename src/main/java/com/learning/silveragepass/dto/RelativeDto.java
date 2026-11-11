package com.learning.silveragepass.dto;

import com.learning.silveragepass.entity.RelationshipType;
import com.learning.silveragepass.entity.RelativeStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RelativeDto {
    private Long id;
    private Long userId;
    private String userName; // 用户名称（发起请求的人）
    private Long relativeId;
    private String relativeName; // 亲人用户名称（接收请求的人）
    private RelationshipType relationshipType;
    private Boolean shareDemands;
    private RelativeStatus status;
    private LocalDateTime createTime;
    private String avatar; // 头像（根据场景可能是发起者或接收者的）
    private String phone; // 电话（根据场景可能是发起者或接收者的）
    private Boolean isInitiator; // 当前用户是否是关系发起方（便于前端判断）
} 