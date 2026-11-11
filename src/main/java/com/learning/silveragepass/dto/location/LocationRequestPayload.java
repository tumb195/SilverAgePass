package com.learning.silveragepass.dto.location;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 位置请求WebSocket消息payload
 * 后端发送给老人App的定位请求
 * Requirements: 3.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationRequestPayload {
    
    /**
     * 请求唯一标识
     */
    private String requestId;
    
    /**
     * 请求者ID（亲属）
     */
    private Long requesterId;
    
    /**
     * 请求者姓名
     */
    private String requesterName;
    
    /**
     * 时间戳
     */
    private Long timestamp;
}
