package com.learning.silveragepass.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 位置获取失败WebSocket消息payload
 * 后端发送给亲属的失败通知
 * Requirements: 3.6, 7.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationFailedPayload {
    
    /**
     * 请求唯一标识
     */
    private String requestId;
    
    /**
     * 老人用户ID
     */
    private Long elderId;
    
    /**
     * 失败原因代码
     * TIMEOUT - 超时
     * OFFLINE - 离线
     * PERMISSION_DENIED - 权限被拒绝
     * LOCATION_ERROR - 定位错误
     */
    private String reason;
    
    /**
     * 失败原因描述
     */
    private String message;
}
